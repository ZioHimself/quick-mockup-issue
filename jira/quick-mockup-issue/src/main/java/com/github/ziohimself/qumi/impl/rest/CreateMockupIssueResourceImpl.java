package com.github.ziohimself.qumi.impl.rest;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.velocity.VelocityManager;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.github.ziohimself.qumi.api.rest.CreateMockupIssueResource;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringWriter;
import java.util.Map;

@Path("/create")
public class CreateMockupIssueResourceImpl implements CreateMockupIssueResource {

    private final VelocityManager velocityManager;
    private final ApplicationProperties applicationProperties;
    private final PageBuilderService pageBuilderService;

    public CreateMockupIssueResourceImpl(@Nonnull VelocityManager velocityManager, @Nonnull ApplicationProperties applicationProperties, @Nonnull PageBuilderService pageBuilderService) {
        this.velocityManager = velocityManager;
        this.applicationProperties = applicationProperties;
        this.pageBuilderService = pageBuilderService;
    }

    @GET
    @AnonymousAllowed
    @Produces({MediaType.TEXT_HTML})
    public Response getCreateForm() {
        final WebResourceAssembler webResourceAssembler = pageBuilderService.assembler();
        RequiredResources requiredResources = webResourceAssembler.resources();
        Supplier<String> resourceTagsSupplier = new Supplier<String>() {@Override public String get() {
            StringWriter stringWriter = new StringWriter();
            webResourceAssembler.assembled().drainIncludedResources().writeHtmlTags(stringWriter, UrlMode.AUTO);
            return stringWriter.toString();
        }};

        Map<String,Object> context = Maps.newHashMap(ImmutableMap.<String, Object>builder()
                        .put("requiredResources", requiredResources)
                        .put("resourceTagsSupplier", resourceTagsSupplier)
                        .build()
        );

        String baseUrl=applicationProperties.getString(APKeys.JIRA_BASEURL);
        String webworkEncoding = applicationProperties.getString(APKeys.JIRA_WEBWORK_ENCODING);

        String renderedText = velocityManager.getEncodedBody("templates/", "qumi-create.vm", baseUrl, webworkEncoding, context);

        return Response.ok(renderedText).build();
    }

    @POST
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    public Response doCreate() {
        return Response.ok(Maps.newHashMap(ImmutableMap.of("foo", "bar"))).build();
    }
}