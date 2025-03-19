package com.sismics.reader.rest.resource;

import com.sismics.reader.core.service.ReportService;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.security.IPrincipal;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Report REST resources.
 *
 * @author copilot
 */
@Path("/report")
public class ReportResource extends BaseResource {

    /**
     * Returns the daily report for the current user.
     * 
     * @return Response
     * @throws JSONException
     */
    @GET
    @Path("/daily")
    @Produces(MediaType.APPLICATION_JSON)
    public Response daily() throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the current user
        IPrincipal principal = (IPrincipal) request.getAttribute(TokenBasedSecurityFilter.PRINCIPAL_ATTRIBUTE);
        String userId = principal.getId();

        // Generate the daily report
        ReportService reportService = new ReportService();
        String reportContent = reportService.generateDailyReport(userId);

        // Check if the report content is meaningful
        if (reportContent == null || reportContent.trim().isEmpty()) {
            JSONObject errorResponse = new JSONObject();
            errorResponse.put("status", "error");
            errorResponse.put("message", "No report content generated. Please try again later.");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }

        // Build the response
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        response.put("content", reportContent);
        return Response.ok().entity(response).build();
    }
}
