package com.bftcom.timesheet.export.utils;

import com.atlassian.jira.bc.issue.search.SearchService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchResults;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.web.bean.PagerFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Утилитные методы для работы с jql
 */
public class JQLUtils {

    public static List<String> getIssueKeysFromJQLFilter(String jqlQuery) {
        SearchService searchService = ComponentAccessor.getComponent(SearchService.class);
        //JiraAuthenticationContext authenticationContext = ComponentAccessor.getJiraAuthenticationContext();
        ApplicationUser user = ComponentAccessor.getUserManager().getUserByName("a.vinakov");
        final SearchService.ParseResult parseResult =
                searchService.parseQuery(user, jqlQuery);
        if (parseResult.isValid()) {
            try {
                final SearchResults results = searchService.search(user,
                        parseResult.getQuery(), PagerFilter.getUnlimitedFilter());
                final List<Issue> issues = results.getIssues();
                List<String> result = new ArrayList<>();
                for (Issue issue : issues) {
                    result.add(issue.getKey());
                }
                return result;
            } catch (SearchException e) {
                //todo log error
                //log.error("Error running search", e);
            }
        } else {
            //log.warn("Error parsing jqlQuery: " + parseResult.getErrors());
        }
        return Collections.emptyList();
    }


}
