package com.github.ziohimself.qumi.api.rest;

//import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

//import javax.ws.rs.GET;
//import javax.ws.rs.POST;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;

//@Path("/create")
public interface CreateMockupIssueResource {
    String SUMMARY_INPUT_PARAM = "summary-input";
    String DESCRIPTION_INPUT_PARAM = "description-input";
    String MOCKUP_IMAGE_URL_INPUT_PARAM = "mockup-image-url-input";

    //    @GET
//    @AnonymousAllowed
//    @Produces({MediaType.TEXT_HTML})
    Response getCreateForm();

//    @POST
//    @AnonymousAllowed
//    @Produces({MediaType.APPLICATION_JSON})
    Response doCreate(
//            @FormParam(SUMMARY_INPUT_PARAM)
            String summary,
//            @FormParam(DESCRIPTION_INPUT_PARAM)
            String description,
//            @FormParam(MOCKUP_IMAGE_URL_INPUT_PARAM)
            String imageUrl
    );
}
