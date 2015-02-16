package com.github.ziohimself.qumi.impl.rest;

import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.velocity.VelocityManager;
import com.atlassian.webresource.api.UrlMode;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import com.atlassian.webresource.api.assembler.RequiredResources;
import com.atlassian.webresource.api.assembler.WebResourceAssembler;
import com.github.ziohimself.qumi.api.rest.CreateMockupIssueResource;
import com.github.ziohimself.qumi.impl.util.Try;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import sun.misc.BASE64Decoder;

import javax.annotation.Nonnull;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

@Path("/create")
public class CreateMockupIssueResourceImpl implements CreateMockupIssueResource {

    private final VelocityManager velocityManager;
    private final ApplicationProperties applicationProperties;
    private final PageBuilderService pageBuilderService;
    private final UserManager userManager;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private final IssueService issueService;
    private final AttachmentService attachmentService;

    public CreateMockupIssueResourceImpl(@Nonnull VelocityManager velocityManager,
                                         @Nonnull ApplicationProperties applicationProperties,
                                         @Nonnull PageBuilderService pageBuilderService,

                                         @Nonnull UserManager userManager,
                                         @Nonnull JiraAuthenticationContext jiraAuthenticationContext,
                                         @Nonnull IssueService issueService,
                                         @Nonnull AttachmentService attachmentService) {
        this.velocityManager = velocityManager;
        this.applicationProperties = applicationProperties;
        this.pageBuilderService = pageBuilderService;
        this.userManager = userManager;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.issueService = issueService;
        this.attachmentService = attachmentService;
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

        String baseUrl=applicationProperties.getString(APKeys.JIRA_BASEURL);
        String webworkEncoding = applicationProperties.getString(APKeys.JIRA_WEBWORK_ENCODING);

        Map<String,Object> context = Maps.newHashMap(ImmutableMap.<String, Object>builder()
                        .put("summaryId", SUMMARY_INPUT_PARAM)
                        .put("descriptionId", DESCRIPTION_INPUT_PARAM)
                        .put("mockupUrlId", MOCKUP_IMAGE_URL_INPUT_PARAM)
                        .put("requiredResources", requiredResources)
                        .put("resourceTagsSupplier", resourceTagsSupplier)
                        .put("baseUrl", baseUrl)
                        .build()
        );

        String renderedText = velocityManager.getEncodedBody("templates/", "qumi-create.vm", baseUrl, webworkEncoding, context);

        return Response.ok(renderedText).build();
    }

    @POST
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    public Response doCreate(@FormParam(SUMMARY_INPUT_PARAM) final String summary, @FormParam("description-input") final String description, @FormParam("mockup-image-url-input") String imageUrl) {
        final Try<ByteArraySupplier> imageBytesSupplier = Try
                .success(imageUrl)
                .flatMap(new Function<String, Try<URL>>() {@Override public Try<URL> apply(String s) {
                    try {
                        URL url = new URL(s);
                        return Try.success(url);
                    } catch (MalformedURLException e) {
                        return Try.failure(e);
                    }
                }})
                .map(new Function<URL, String>() {@Override public String apply(URL url) {
                    return url.getPath();
                }})
                .flatMap(new Function<String, Try<ByteArraySupplier>>() {@Override public Try<ByteArraySupplier> apply(String imageBase64Str) {
                    try {
                        BASE64Decoder base64Decoder = new BASE64Decoder();
                        final byte[] imageBytes = base64Decoder.decodeBuffer(imageBase64Str);
                        return Try.<ByteArraySupplier>success(new ByteArraySupplier() {@Override public byte[] get() {
                            return imageBytes;
                        }});
                    } catch (IOException e) {
                        return Try.failure(e);
                    }
                }});

        final Try<ApplicationUser> loggedInAdmin = Try
                .success(userManager.getUserByKey("admin"))
                .flatMap(new Function<ApplicationUser, Try<ApplicationUser>>() {@Override public Try<ApplicationUser> apply(ApplicationUser applicationUser) {
                    if (applicationUser == null) {
                        return Try.failure(new NullPointerException("`admin` user is `null`"));
                    }
                    return Try.success(applicationUser);
                }})
                .map(new Function<ApplicationUser, ApplicationUser>() {@Override public ApplicationUser apply(ApplicationUser applicationUser) {
                    jiraAuthenticationContext.setLoggedInUser(applicationUser);
                    return applicationUser;
                }});

        Try<Issue> issue = Try
                .success(true).flatMap(new Function<Boolean, Try<Issue>>() {@Override public Try<Issue> apply(Boolean aBoolean) {
                    if (imageBytesSupplier.isFailure() || loggedInAdmin.isFailure()) {
                        return Try.failure("Either not able to read or not able log in the user");
                    }
                    ByteArraySupplier imageByteArraySupplier = imageBytesSupplier.get();
                    ApplicationUser admin = loggedInAdmin.get();
                    IssueInputParameters issueInputParameters = issueService.newIssueInputParameters()
                            .setSummary(summary)
                            .setDescription(description);
                    IssueService.CreateValidationResult createValidationResult = issueService.validateCreate(admin.getDirectoryUser(), issueInputParameters);
                    if (!createValidationResult.isValid()) {
                        return Try.failure("" + createValidationResult.getErrorCollection());
                    }
                    Issue issue = createValidationResult.getIssue();
                    return Try.success(issue);
                }});

        return Response.ok(Maps.newHashMap(ImmutableMap.builder()
                .put(SUMMARY_INPUT_PARAM, summary)
                .put(DESCRIPTION_INPUT_PARAM, description)
                .put(MOCKUP_IMAGE_URL_INPUT_PARAM, imageUrl)
                .build()
        )).build();
    }

    interface ByteArraySupplier {
        byte[] get();
    }
}