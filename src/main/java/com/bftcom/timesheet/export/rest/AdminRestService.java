package com.bftcom.timesheet.export.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.customfields.manager.OptionsManager;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.customfields.option.Options;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.bftcom.timesheet.export.utils.Constants;
import com.bftcom.timesheet.export.utils.JQLUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 *
 */
@Path("/admin")
public class AdminRestService {

    @DELETE
    @Path("/financeproject")
    @AnonymousAllowed
    @Produces({MediaType.APPLICATION_JSON})
    public Response deleteFinanceProject(@QueryParam("name") String name, @QueryParam("onlyDuplicates") boolean onlyDuplicates) {
        OptionsManager optionsManager = ComponentAccessor.getOptionsManager();
        CustomField financeProjectField = ComponentAccessor.getCustomFieldManager().getCustomFieldObjectByName(Constants.financeProjectFieldName);
        Options options = getOptionsFor(financeProjectField);
        List<Option> optionList = optionsManager.findByOptionValue(name);
        int count = 0;
        if (onlyDuplicates) {
            Option option = options.getOptionForValue(name, null);
            for (Option o : optionList) {
                if (!o.getDisabled() && o.getOptionId() != null && option.getOptionId() != null && !o.getOptionId().equals(option.getOptionId()) && !optionHasIssues(o)) {
                    optionsManager.deleteOptionAndChildren(o);
                    count++;
                }
            }
        } else {
            for (Option o : optionList) {
                optionsManager.deleteOptionAndChildren(o);
                count++;
            }
        }
        return Response.ok(count).build();
    }

    private boolean optionHasIssues(Option option) {
        return JQLUtils.getIssueKeysFromJQLFilter("'Бюджет проекта ПУ' = '" + option.getValue() + "'").size() == 0;
    }

    private Options getOptionsFor(CustomField customField) {
        return ComponentAccessor.getOptionsManager().getOptions(customField.getConfigurationSchemes().listIterator().next().getOneAndOnlyConfig());
    }

}
