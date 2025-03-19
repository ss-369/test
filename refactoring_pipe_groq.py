#!/usr/bin/env python3
import os
from groq import Groq
from github import Github
from datetime import datetime
import logging
from typing import List, Dict
import time
import tempfile
import lizard

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

# Configuration from environment variables
GROQ_API_KEY = os.environ["GROQ_API_KEY"]  # Your Groq API key
GITHUB_TOKEN = os.environ["GITHUB_TOKEN"]
REPO_NAME = os.environ["GITHUB_REPOSITORY"]  # e.g. "SE-course-serc/project-1-team-9"
TARGET_FOLDER = os.environ.get("TARGET_FOLDER", "reader-core/src/main/java/com/sismics/reader/core/dao/lucene")
BASE_BRANCH = os.environ.get("BASE_BRANCH", "master")

# Initialize Groq client
client = Groq(api_key=GROQ_API_KEY)

# Configure retry parameters for API calls
MAX_RETRIES = 3
RETRY_DELAY = 5  # seconds

def analyze_metrics(file_content: str) -> Dict:
    """Analyze code metrics using lizard."""
    try:
        with tempfile.NamedTemporaryFile(mode='w+', suffix='.java', delete=False) as temp:
            temp.write(file_content)
            temp.flush()
            analysis = lizard.analyze_file(temp.name)
        function_count = len(analysis.function_list)
        total_cyclomatic = sum(func.cyclomatic_complexity for func in analysis.function_list)
        avg_cyclomatic = total_cyclomatic / function_count if function_count > 0 else 0
        return {
            "nloc": analysis.nloc,
            "function_count": function_count,
            "avg_cyclomatic": avg_cyclomatic
        }
    except Exception as e:
        logging.error(f"Error analyzing metrics: {str(e)}")
        return {"nloc": 0, "function_count": 0, "avg_cyclomatic": 0}

def get_file_content(repo, path: str) -> str:
    """Get the content of a file from GitHub."""
    try:
        file_content = repo.get_contents(path, ref=BASE_BRANCH)
        if isinstance(file_content, list):
            return None
        return file_content.decoded_content.decode('utf-8')
    except Exception as e:
        logging.error(f"Error reading file {path}: {str(e)}")
        return None

def get_java_files_in_folder(repo, folder_path: str) -> List[Dict]:
    """Get all Java files in the specified folder."""
    files = []
    try:
        contents = repo.get_contents(folder_path, ref=BASE_BRANCH)
        logging.info(f"Scanning folder: {folder_path}")
        for content in contents:
            if content.type == "file" and content.path.endswith('.java'):
                logging.info(f"Found Java file: {content.path}")
                code = get_file_content(repo, content.path)
                if code:
                    files.append({
                        'path': content.path,
                        'content': code,
                        'sha': content.sha
                    })
    except Exception as e:
        logging.error(f"Error accessing folder {folder_path}: {str(e)}")
    return files

def make_api_request(messages, retries=MAX_RETRIES):
    """Make an API request using Groq with exponential backoff retry logic."""
    for attempt in range(retries):
        try:
            response = client.chat.completions.create(
                model="llama-3.3-70b-versatile",  # Using Groq's Llama-3.3 70B versatile model
                messages=messages,
                temperature=0.7,
                max_tokens=2000
            )
            return response
        except Exception as e:
            error_str = str(e).lower()
            wait_time = RETRY_DELAY * (2 ** attempt)
            if "too many requests" in error_str or "rate limit" in error_str:
                logging.warning(f"Rate limit encountered. Retrying in {wait_time} seconds... (Attempt {attempt + 1}/{retries})")
            else:
                logging.warning(f"API request failed, retrying in {wait_time} seconds... (Attempt {attempt + 1}/{retries})")
            time.sleep(wait_time)
    raise Exception("API request failed after maximum retries")

def detect_design_smells(files: List[Dict]) -> List[Dict]:
    """Detect design smells in the provided Java files using Groq."""
    smells = []
    # List of common design smells
    design_smells_list = [
        "feature_envy", "inappropriate_intimacy", "shotgun_surgery", "parallel_inheritance",
        "divergent_change", "data_class", "primitive_obsession", "long_method",
        "large_class", "long_parameter_list", "switch_statements", "temporary_field",
        "message_chains", "middle_man", "speculative_generality", "refused_bequest",
        "lazy_class", "duplicate_code", "comments", "god_class", "spaghetti_code",
        "odd_loop_structure", "complex_conditional", "magic_numbers", "global_data",
        "deep_nesting", "excessive_return_points", "dead_code", "hard_coding", "tight_coupling"
    ]
    
    for i in range(len(files)):
        for j in range(i + 1, len(files)):
            file1 = files[i]
            file2 = files[j]
            logging.info(f"Analyzing pair:\n - File 1: {file1['path']}\n - File 2: {file2['path']}")
            # Compute metrics for both files
            metrics1 = analyze_metrics(file1['content'])
            metrics2 = analyze_metrics(file2['content'])
            
            prompt = f"""Compare these two Java files for design smells:
File 1 ({file1['path']}):
{file1['content']}
Metrics: NLOC={metrics1['nloc']}, Function Count={metrics1['function_count']}, Avg Cyclomatic={metrics1['avg_cyclomatic']}

File 2 ({file2['path']}):
{file2['content']}
Metrics: NLOC={metrics2['nloc']}, Function Count={metrics2['function_count']}, Avg Cyclomatic={metrics2['avg_cyclomatic']}

Look for the following design smells: {', '.join(design_smells_list)}.
Format each finding on a new line as:
<smell_type>: <brief_description>
If no smells are found, reply exactly with: "No design smells detected"
"""
            try:
                response = make_api_request([
                    {"role": "system", "content": "You are a code reviewer focusing on design smells and code metrics."},
                    {"role": "user", "content": prompt}
                ])
                raw_response = response.choices[0].message.content.strip()
                logging.info(f"Raw API response: {raw_response}")
                if raw_response.strip().lower() == "no design smells detected":
                    continue
                for line in raw_response.splitlines():
                    if ":" in line:
                        try:
                            smell_type, description = line.split(": ", 1)
                            smell_type = smell_type.strip().lower().replace(' ', '_')
                            smell_info = {
                                'type': smell_type,
                                'files': [file1['path'], file2['path']],
                                'description': description.strip(),
                                'file_contents': {
                                    file1['path']: file1,
                                    file2['path']: file2
                                },
                                'metrics': {
                                    file1['path']: metrics1,
                                    file2['path']: metrics2
                                }
                            }
                            smells.append(smell_info)
                            logging.info(f"Detected smell: {smell_type}")
                        except ValueError:
                            logging.error(f"Unexpected format in line: {line}")
            except Exception as e:
                logging.error(f"Error analyzing files: {str(e)}")
    return smells

def refactor_code(smell: Dict) -> Dict[str, Dict]:
    """Refactor code based on the identified design smell using Groq."""
    refactored_files = {}
    for file_path, file_info in smell['file_contents'].items():
        logging.info(f"Refactoring file: {file_path}")
        prompt = f"""Refactor this Java code to fix {smell['type']}:
Issue: {smell['description']}

Original code:
{file_info['content']}

Requirements:
- Preserve functionality
- Follow SOLID principles
- Improve organization
- Reduce coupling
Return only the refactored code."""
        try:
            response = make_api_request([
                {"role": "system", "content": "You are a Java refactoring expert."},
                {"role": "user", "content": prompt}
            ])
            refactored_code = response.choices[0].message.content.strip()
            if refactored_code:
                refactored_files[file_path] = {
                    'content': refactored_code,
                    'sha': file_info['sha']
                }
                logging.info(f"Successfully refactored {file_path}")
            else:
                logging.warning(f"Empty refactored code received for {file_path}")
        except Exception as e:
            logging.error(f"Error refactoring file {file_path}: {str(e)}")
    return refactored_files

def create_pull_request(repo, refactored_files: Dict[str, Dict], smell: Dict) -> str:
    """Create a pull request with the refactored code."""
    try:
        sanitized_smell = smell['type'].strip().lower().replace(' ', '-').replace('_', '-')
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        new_branch = f"refactor/{sanitized_smell}/{timestamp}"
        base_sha = repo.get_branch(BASE_BRANCH).commit.sha
        repo.create_git_ref(ref=f"refs/heads/{new_branch}", sha=base_sha)
        
        for file_path, file_info in refactored_files.items():
            repo.update_file(
                file_path,
                f"Refactor: Fix {smell['type']} in {file_path}",
                file_info['content'],
                file_info['sha'],
                branch=new_branch
            )
        
        # Format metrics for the PR description
        metrics_info = ""
        if "metrics" in smell:
            metrics_info = "\nMetrics:\n" + "\n".join([
                f"{path}: NLOC={metrics['nloc']}, Function Count={metrics['function_count']}, Avg Cyclomatic={metrics['avg_cyclomatic']}"
                for path, metrics in smell["metrics"].items()
            ])
        
        pr_title = f"Refactor: Fix {smell['type']}"
        pr_body = f"""Automated refactoring to address design smell
Type: {smell['type']}
Files affected: {', '.join(smell['files'])}
Description: {smell['description']}
{metrics_info}

This pull request was automatically generated by the refactoring pipeline.
Please review the changes carefully before merging.
"""
        pr = repo.create_pull(
            title=pr_title,
            body=pr_body,
            base=BASE_BRANCH,
            head=new_branch
        )
        logging.info(f"Pull Request created: {pr.html_url}")
        return pr.html_url
    except Exception as e:
        logging.error(f"Error creating pull request: {str(e)}")
        return None

def main():
    try:
        g = Github(GITHUB_TOKEN)
        repo = g.get_repo(REPO_NAME)
        files = get_java_files_in_folder(repo, TARGET_FOLDER)
        if not files:
            logging.warning("No Java files found in the specified folder")
            return
        
        smells = detect_design_smells(files)
        logging.info(f"Found {len(smells)} design smells")
        
        # If more than 2 design smells, process only the first 2.
        if len(smells) > 2:
            logging.info("More than 2 design smells detected. Processing only the first 2.")
            smells = smells[:2]
        
        for smell in smells:
            refactored_files = refactor_code(smell)
            if refactored_files:
                pr_url = create_pull_request(repo, refactored_files, smell)
                if pr_url:
                    logging.info(f"Successfully created pull request: {pr_url}")
                else:
                    logging.error("Failed to create pull request")
    except Exception as e:
        logging.error(f"Error in main execution: {str(e)}")
        raise e

if __name__ == "__main__":
    main()
