Ext.define('reader.controller.Main', {
    // ...existing code...

    init: function () {
        // ...existing code...

        // Add daily report button to the toolbar
        var toolbar = Ext.ComponentQuery.query('toolbar[region=north]')[0];
        toolbar.add({
            xtype: 'button',
            text: 'Daily Report',
            id: 'daily-report-button',
            iconCls: 'report-icon',
            handler: function () {
                r.report.showDailyReport();
            }
        });

        // Create the modal for displaying reports
        Ext.create('Ext.window.Window', {
            title: 'Daily Article Summary',
            id: 'daily-report-modal',
            width: 600,
            height: 400,
            closeAction: 'hide',
            autoScroll: true,
            modal: true,
            layout: 'fit',
            items: [{
                xtype: 'panel',
                html: '<div id="daily-report-content" class="report-content"></div>' +
                    '<div id="daily-report-spinner" class="spinner"></div>'
            }]
        });

        // ...existing code...
    }

    // ...existing code...
});
