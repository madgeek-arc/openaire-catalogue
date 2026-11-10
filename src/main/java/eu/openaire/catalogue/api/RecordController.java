package eu.openaire.catalogue.api;

import eu.openaire.catalogue.resource.BundleService;
import eu.openaire.catalogue.resource.Bundles;
import eu.openaire.catalogue.resource.Channels;
import eu.openaire.catalogue.resource.ResourceKind;
import eu.openaire.catalogue.security.AccessRules;
import gr.uoa.di.madgik.registry.annotation.BrowseParameters;
import gr.uoa.di.madgik.registry.domain.FacetFilter;
import gr.uoa.di.madgik.registry.domain.Paging;
import gr.uoa.di.madgik.registry.exception.ResourceException;
import gr.uoa.di.madgik.registry.exception.ResourceNotFoundException;
import gr.uoa.di.madgik.resourcecatalogue.dto.UserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Routes shared by providers, services and datasources, for one channel. Public reads return payloads of active records
 * on the channel; owners and reviewers also read pending records and records on other channels, and their bundles. New
 * records are listed on the channel of the API they are added through. Access is checked with {@code @PreAuthorize}
 * against {@link AccessRules}, except for single-record reads, which depend on the stored record.
 */
public abstract class RecordController {

    protected final ResourceKind kind;
    protected final String channel;
    protected final BundleService bundles;
    protected final AccessRules access;

    protected RecordController(ResourceKind kind, String channel, BundleService bundles, AccessRules access) {
        this.kind = kind;
        this.channel = channel;
        this.bundles = bundles;
        this.access = access;
    }

    /**
     * The record type of this controller.
     */
    public ResourceKind getKind() {
        return kind;
    }

    @Operation(summary = "Returns the record with the given id. Records that are not active are visible to those who "
            + "manage them only.")
    @GetMapping("{id}")
    public Map<String, Object> get(@PathVariable String id, @Parameter(hidden = true) Authentication auth) {
        Map<String, Object> payload = Bundles.payload(readable(id, auth), kind);
        return access.canManage(auth, kind, id) ? payload : publicView(payload);
    }

    @Operation(summary = "Returns the active records on the channel of this API, based on a set of filters.")
    @BrowseParameters
    @GetMapping
    public Paging<Map<String, Object>> getAll(@Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        FacetFilter filter = publicFilter(FacetFilter.from(params));
        return bundles.search(kind, filter).map(bundle -> publicView(Bundles.payload(bundle, kind)));
    }

    @Operation(summary = "Returns the bundle of the record with the given id, with its status and logging.")
    @GetMapping("bundles/{id}")
    @PreAuthorize("@access.canManage(authentication, #root.this.kind, #id)")
    public ResponseEntity<Map<String, Object>> getBundle(@PathVariable String id) {
        Map<String, Object> bundle = bundles.get(kind, id);
        return withVersion(ResponseEntity.ok(), bundle).body(bundle);
    }

    @Operation(summary = "Returns the bundles of every record, whatever its status or channels, based on a set of "
            + "filters. Reviewers only.")
    @BrowseParameters
    @GetMapping("bundles")
    @PreAuthorize("@access.isReviewer(authentication)")
    public Paging<Map<String, Object>> getAllBundles(
            @Parameter(hidden = true) @RequestParam MultiValueMap<String, Object> params) {
        return bundles.search(kind, FacetFilter.from(params));
    }

    @Operation(summary = "Registers a new record, listed on the channel of this API. It stays pending, and hidden, "
            + "until a reviewer approves it.")
    @PostMapping
    @PreAuthorize("@access.canAdd(authentication, #root.this.kind, #payload)")
    public ResponseEntity<Map<String, Object>> add(@RequestBody LinkedHashMap<String, Object> payload,
                                                   @RequestParam MultiValueMap<String, String> params,
                                                   @Parameter(hidden = true) Authentication auth) {
        beforeAdd(payload, params, auth);
        Map<String, Object> stored = bundles.create(kind, payload, channel, UserInfo.of(auth));
        return new ResponseEntity<>(Bundles.payload(stored, kind), HttpStatus.CREATED);
    }

    @Operation(summary = "Replaces the payload of the record with the given id. A payload id, if present, must match it. "
            + "Its status and activity are kept, except that a rejected record becomes pending again. With If-Match set "
            + "to the metadata.modifiedAt of the bundle last read, the update fails with 412 if the record changed "
            + "since. The response ETag is the new version.")
    @PutMapping("{id}")
    @PreAuthorize("@access.canManage(authentication, #root.this.kind, #id)")
    public ResponseEntity<Map<String, Object>> update(@PathVariable String id,
                                                      @RequestBody LinkedHashMap<String, Object> payload,
                                                      // A query parameter, not a body field: the body is the record
                                                      // itself, and the comment is not part of it but goes into the
                                                      // logging entry of the update.
                                                      @RequestParam(required = false) String comment,
                                                      @RequestHeader(value = HttpHeaders.IF_MATCH, required = false)
                                                      String ifMatch,
                                                      @Parameter(hidden = true) Authentication auth) {
        Object bodyId = payload.get(Bundles.ID);
        if (bodyId != null && !id.equals(bodyId.toString())) {
            throw new ResourceException("The payload id '" + bodyId + "' does not match '" + id + "'",
                    HttpStatus.BAD_REQUEST);
        }
        payload.put(Bundles.ID, id);
        Map<String, Object> stored = bundles.update(kind, payload, comment, UserInfo.of(auth), access.isReviewer(auth),
                unquote(ifMatch));
        return withVersion(ResponseEntity.ok(), stored).body(Bundles.payload(stored, kind));
    }

    @Operation(summary = "Deletes the record with the given id.")
    @DeleteMapping("{id}")
    @PreAuthorize("@access.canManage(authentication, #root.this.kind, #id)")
    public Map<String, Object> delete(@PathVariable String id, @Parameter(hidden = true) Authentication auth) {
        return Bundles.payload(bundles.delete(kind, id, UserInfo.of(auth)), kind);
    }

    /**
     * The optional body of a review action. The comment is recorded in the logging entry of the action.
     */
    public record ActionRequest(String comment) {

        static String commentOf(ActionRequest request) {
            return request != null ? request.comment() : null;
        }
    }

    @Operation(summary = "Approves the record with the given id, which makes it active. A service or datasource is "
            + "approved only under an approved, active provider or service. Reviewers only.")
    @PostMapping("bundles/{id}/approve")
    @PreAuthorize("@access.isReviewer(authentication)")
    public Map<String, Object> approve(@PathVariable String id, @RequestBody(required = false) ActionRequest request,
                                       @Parameter(hidden = true) Authentication auth) {
        return bundles.approve(kind, id, ActionRequest.commentOf(request), UserInfo.of(auth));
    }

    @Operation(summary = "Rejects the record with the given id, which makes it inactive. Reviewers only.")
    @PostMapping("bundles/{id}/reject")
    @PreAuthorize("@access.isReviewer(authentication)")
    public Map<String, Object> reject(@PathVariable String id, @RequestBody(required = false) ActionRequest request,
                                      @Parameter(hidden = true) Authentication auth) {
        return bundles.reject(kind, id, ActionRequest.commentOf(request), UserInfo.of(auth));
    }

    @Operation(summary = "Activates the record with the given id. Only an approved record can be activated. "
            + "Reviewers only.")
    @PostMapping("bundles/{id}/activate")
    @PreAuthorize("@access.isReviewer(authentication)")
    public Map<String, Object> activate(@PathVariable String id, @RequestBody(required = false) ActionRequest request,
                                        @Parameter(hidden = true) Authentication auth) {
        return bundles.setActive(kind, id, true, ActionRequest.commentOf(request), UserInfo.of(auth));
    }

    @Operation(summary = "Deactivates the record with the given id. Reviewers only.")
    @PostMapping("bundles/{id}/deactivate")
    @PreAuthorize("@access.isReviewer(authentication)")
    public Map<String, Object> deactivate(@PathVariable String id, @RequestBody(required = false) ActionRequest request,
                                          @Parameter(hidden = true) Authentication auth) {
        return bundles.setActive(kind, id, false, ActionRequest.commentOf(request), UserInfo.of(auth));
    }

    @Operation(summary = "Adds the record to a channel. Adding it to a channel it is on changes nothing. Reviewers "
            + "only.")
    @PutMapping("bundles/{id}/channels/{channel}")
    @PreAuthorize("@access.isReviewer(authentication)")
    public List<String> addChannel(@PathVariable String id, @PathVariable String channel,
                                   @Parameter(hidden = true) Authentication auth) {
        return bundles.setChannel(kind, id, channel, true, UserInfo.of(auth));
    }

    @Operation(summary = "Removes the record from a channel. Removing it from a channel it is not on changes nothing. "
            + "Reviewers only.")
    @DeleteMapping("bundles/{id}/channels/{channel}")
    @PreAuthorize("@access.isReviewer(authentication)")
    public List<String> removeChannel(@PathVariable String id, @PathVariable String channel,
                                      @Parameter(hidden = true) Authentication auth) {
        return bundles.setChannel(kind, id, channel, false, UserInfo.of(auth));
    }

    private static ResponseEntity.BodyBuilder withVersion(ResponseEntity.BodyBuilder response,
                                                          Map<String, Object> bundle) {
        String version = Bundles.version(bundle);
        return version != null ? response.eTag("\"" + version + "\"") : response;
    }

    /**
     * @return the value without surrounding quotes and a weak-validator prefix, or {@code null} for a missing or
     * wildcard value
     */
    static String unquote(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank() || ifMatch.trim().equals("*")) {
            return null;
        }
        String value = ifMatch.trim();
        if (value.startsWith("W/")) {
            value = value.substring(2);
        }
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * Limits a public listing to the active records on the channel, overriding any value the client sent for the same
     * filters.
     */
    protected FacetFilter publicFilter(FacetFilter filter) {
        return Channels.restrict(filter, channel);
    }

    /**
     * The payload as shown to those who do not manage the record.
     */
    protected Map<String, Object> publicView(Map<String, Object> payload) {
        return payload;
    }

    /**
     * Checks that apply to new records of one type only.
     */
    protected void beforeAdd(Map<String, Object> payload, MultiValueMap<String, String> params, Authentication auth) {
    }

    /**
     * @throws ResourceNotFoundException if the record does not exist, or the user may not read it: a record outside the
     *                                   channel is read by those who manage it only
     */
    protected Map<String, Object> readable(String id, Authentication auth) {
        Map<String, Object> bundle = bundles.get(kind, id);
        if (!access.canRead(auth, kind, bundle)) {
            throw new ResourceNotFoundException(id, kind.type());
        }
        return access.canManage(auth, kind, id) ? bundle : Channels.require(bundle, kind, channel);
    }
}
