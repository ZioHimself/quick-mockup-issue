package com.github.ziohimself.qumi.api.rest;

//import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

//import javax.ws.rs.GET;
//import javax.ws.rs.POST;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//@Path("/create")
public interface CreateMockupIssueResource {
//    @GET
//    @AnonymousAllowed
//    @Produces({MediaType.TEXT_HTML})
    Response getCreateForm();

//    @POST
//    @AnonymousAllowed
//    @Produces({MediaType.APPLICATION_JSON})
    Response doCreate();
}
