<#ftl output_format="HTML">
<p>Dear user,</p>
<#if action == "created">
<p>Your application for registering [${name}] as a new ${project} ${type} has been received and is under review.
    You will be notified when it has been reviewed.</p>
<#elseif action == "approved">
<p>Your application for registering [${name}] as a new ${project} ${type} has been approved. It is now listed on the
    ${project} Portal.</p>
<#else>
<p>Your application for registering [${name}] as a new ${project} ${type} has been rejected.</p>
</#if>
<#if comment?has_content>
<p>Comment: ${comment}</p>
</#if>
<#if homepage?has_content>
<p><a href="${homepage}">${homepage}</a></p>
</#if>
<p>Best Regards,<br>the ${project} Team</p>
