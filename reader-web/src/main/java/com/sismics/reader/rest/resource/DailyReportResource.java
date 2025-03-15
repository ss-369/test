package com.sismics.reader.rest.resource;

import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.service.DailyReportGenerator;
import com.sismics.reader.core.service.GeminiDailyReportGenerator;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoint for generating a daily report.
 * When a client invokes this resource, it generates a daily summary report
 * for the current user.
 */
@Path("/dailyReport")
public class DailyReportResource {

    private DailyReportGenerator reportGenerator = new GeminiDailyReportGenerator();

    /**
     * GET endpoint to generate a daily report.
     * In a real system, the user would be determined via session/authentication.
     * Here we use a dummy admin user.
     * @return Response containing the generated report.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getDailyReport() {
        // Create a dummy user for demonstration. In your system, use the authenticated user.
        User dummyUser = new User();
        dummyUser.setUsername("admin");
        dummyUser.setPassword("admin"); // For demo purposes only.

        String report = reportGenerator.generateDailyReport(dummyUser);
        return Response.ok(report).build();
    }
}
