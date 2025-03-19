/**
 * Reports module.
 */
r.report = {
    /**
     * Parse markdown text to HTML
     * Simple parser for basic markdown features
     */
    parseMarkdown: function (text) {
        if (!text) return '';

        // Process code blocks
        text = text.replace(/```([^`]+)```/g, '<pre><code>$1</code></pre>');

        // Process headers
        text = text.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        text = text.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        text = text.replace(/^# (.*$)/gim, '<h1>$1</h1>');

        // Process unordered lists: capture consecutive lines starting with "* "
        text = text.replace(/((?:^\* .+(?:\n|$))+)/gm, function (match) {
            let listItems = match.split('\n')
                .filter(line => line.trim() !== '')
                .map(item => item.replace(/^\* (.+)$/, '<li>$1</li>'))
                .join('');
            return '<ul>' + listItems + '</ul>';
        });

        // Process ordered lists: capture consecutive lines starting with a number followed by ". "
        text = text.replace(/((?:^\d+\. .+(?:\n|$))+)/gm, function (match) {
            let listItems = match.split('\n')
                .filter(line => line.trim() !== '')
                .map(item => item.replace(/^\d+\. (.+)$/, '<li>$1</li>'))
                .join('');
            return '<ol>' + listItems + '</ol>';
        });

        // Process emphasis (bold, italic)
        text = text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
        text = text.replace(/\*(.*?)\*/g, '<em>$1</em>');

        // Process links
        text = text.replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank">$1</a>');

        // Process paragraphs (replace two or more newlines with paragraph breaks)
        text = '<p>' + text.replace(/\n{2,}/g, '</p><p>') + '</p>';

        // Process single newlines as line breaks
        text = text.replace(/\n/g, '<br>');

        return text;
    },

    /**
     * Generate and show daily report.
     */
    showDailyReport: function () {
        // Show loading indicator
        r.util.showSpinner('daily-report-spinner');

        // Request the daily report from the server
        $.ajax({
            type: 'GET',
            url: r.util.url.report_daily,
            dataType: 'json',
            timeout: 90000  // Changed to 90 seconds (90000ms)
        })
            .done(function (data) {
                // Hide loading indicator
                r.util.hideSpinner('daily-report-spinner');

                if (data.status === 'ok') {
                    // Parse markdown content
                    let content = r.report.parseMarkdown(data.content);

                    // Create a modal dialog with the content
                    let dialog = $('<div id="daily-report-dialog" title="Daily Article Summary">' +
                        '<div id="daily-report-content" class="report-content">' + content + '</div>' +
                        '</div>');

                    // Show dialog with jQuery UI if available, otherwise fallback
                    if (typeof dialog.dialog === 'function') {
                        dialog.dialog({
                            modal: true,
                            width: 600,
                            height: 500,
                            buttons: {
                                "Close": function () {
                                    $(this).dialog("close");
                                }
                            }
                        });
                    } else {
                        // Fallback if jQuery UI dialog is not available.
                        $('body').append(dialog);
                        dialog.css({
                            position: 'fixed',
                            top: '10%',
                            left: '20%',
                            width: '60%',
                            maxHeight: '80%',
                            background: '#fff',
                            border: '1px solid #ccc',
                            padding: '15px',
                            zIndex: 1000,
                            overflowY: 'auto'
                        });

                        // Add a close button with more styling
                        $('<div class="report-controls" style="text-align: right; margin-top: 10px;"></div>')
                            .appendTo(dialog)
                            .append(
                                $('<button>Close</button>')
                                    .css({
                                        padding: '5px 10px',
                                        cursor: 'pointer'
                                    })
                                    .click(function () { dialog.remove(); })
                            );
                    }
                } else {
                    r.util.showError('Error loading daily report: ' + (data.message || 'Unknown error'));
                }
            })
            .fail(function (jqXHR, textStatus, errorThrown) {
                r.util.hideSpinner('daily-report-spinner');
                r.util.showError('Failed to load daily report: ' + (textStatus || 'Unknown error'));
                console.error('Report loading failed:', errorThrown);
            });
    },

    /**
     * Initialize report module.
     */
    init: function () {
        // Make the daily report button visible
        $('#daily-report-button').removeClass('hidden');

        // Register click handlers
        $('#daily-report-button').click(function () {
            r.report.showDailyReport();
        });
    }
};

// Initialize when the document is ready.
$(document).ready(function () {
    r.report.init();
});
