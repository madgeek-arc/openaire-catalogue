<#ftl output_format="HTML">
<p>Dear ${project} Team,</p>
<#if action == "created">
<p>A new application for registering [${name}] - ([${id}]) as a new ${project} ${type} has been received and should be
    reviewed.</p>
<#elseif action == "resubmitted">
<p>The rejected application of [${name}] - ([${id}]) for registering as a new ${project} ${type} has been updated and
    should be reviewed again.</p>
<#elseif action == "approved">
<p>The application of [${name}] - ([${id}]) for registering as a new ${project} ${type} has been approved.</p>
<#else>
<p>The application of [${name}] - ([${id}]) for registering as a new ${project} ${type} has been rejected.</p>
</#if>
<#if comment?has_content>
<p>Comment: ${comment}</p>
</#if>
<#if homepage?has_content>
<p><a href="${homepage}">${homepage}</a></p>
</#if>
<p>Best Regards,<br>the ${project} Team</p>
