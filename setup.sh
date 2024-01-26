# Create a new directory for your project
mkdir YourProject

# Navigate into the project directory
cd YourProject

# Initialize a new Git repository
git init

# Create some files in your project directory
touch file1.txt file2.txt

# Add the files to the staging area
git add .

# Commit the changes with a commit message
git commit -m "Initial commit"

# Create a new repository on GitHub (replace 'Your-Username' with your GitHub username and 'YourRepo' with your desired repository name)
# Note: You can create the repository on the GitHub website if you prefer
curl -u Your-Username https://api.github.com/user/repos -d '{"name":"YourRepo"}'

# Add the GitHub repository as a remote
git remote add origin https://github.com/Your-Username/YourRepo.git

# Push the repository to GitHub (set the upstream branch for the first push)
git push -u origin master
