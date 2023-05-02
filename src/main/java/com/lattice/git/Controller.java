package com.lattice.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class Controller {

        private SheetsQuickstart sheetsQuickstart=new SheetsQuickstart();

    /* API_DOC : Updates the given spreadsheet with the all the issues in the repository */
    @PostMapping("/retrieveIssues")
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(value = "Adds all issues from Github Repo to Google Sheet.")
    public ResponseEntity<String> addAllRepoIssuesToSheet(
            @RequestParam(defaultValue = "QuizWorld", value = "Name of Repository") String repoName,
            @RequestParam(defaultValue = "anuragrawat7", value = "Owner/Organization name for repository") String ownerName,
            @RequestParam(required = false, defaultValue = "all", value = "Filter issues based on their state") StringBuilder filter,
            @RequestParam String spreadsheetId,
            @RequestParam(required = false, value = "Name of specific sheet in SpreadSheet") StringBuilder sheetName,
            @RequestParam(required = true, value = "Github Token") StringBuilder githubToken) throws IOException, GeneralSecurityException {

        if (filter == null) {
            filter = new StringBuilder();
            filter.append("all");
        }

        CloseableHttpClient httpClient = HttpClients.createDefault();
        String closedIssuesGetRequest = "https://api.github.com/search/issues?q=repo:" + ownerName + "/" + repoName + "+type:issue+state:closed";
        String openIssueGetRequest = "https://api.github.com/search/issues?q=repo:" + ownerName + "/" + repoName + "+type:issue+state:open";
        boolean tokenExists = githubToken != null || !githubToken.toString().trim().equals("");


        HttpGet httpGetClosedIssues = new HttpGet(closedIssuesGetRequest);
        HttpGet httpGetOpenIssues = new HttpGet(openIssueGetRequest);
        if (tokenExists) {
            httpGetClosedIssues.addHeader("Authorization", "token " + githubToken);
            httpGetOpenIssues.addHeader("Authorization", "token " + githubToken);
        }
        CloseableHttpResponse closeableHttpResponseClosedIssues = httpClient.execute(httpGetClosedIssues);
        CloseableHttpResponse closeableHttpResponseOpenIssues = httpClient.execute(httpGetOpenIssues);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode postResponseClosedIssues = mapper.readTree(closeableHttpResponseClosedIssues.getEntity().getContent());
        JsonNode postResponseOpenIssues = mapper.readTree(closeableHttpResponseOpenIssues.getEntity().getContent());

        int apiCalls;
        try {

            int closedIssues = Integer.parseInt(
                    postResponseClosedIssues.get("total_count").toString());
            int openIssues = Integer.parseInt(
                    postResponseOpenIssues.get("total_count").toString());

            apiCalls = (closedIssues + openIssues) / 100 + 1;
        } catch (NullPointerException ne) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Repository inaccessible. Please make sure you have provided correct token");
        }
        List<List<Object>> issues = new ArrayList<>();
        List headers = new ArrayList<>();
        headers.add("Issue Number");
        headers.add("Title");
        headers.add("Created By");
        headers.add("State");
        headers.add("Milestone");
        headers.add("Assignees");
        headers.add("Labels");
        headers.add("Created at");
        headers.add("Closed at");
        headers.add("Reopen count");
        issues.add(headers);
        for (int i = 1; i <= apiCalls; i++) {
            String getRequest = "https://api.github.com/repos/"
                    + ownerName
                    + "/"
                    + repoName
                    + "/issues?state="
                    + filter + "&page=" + i + "&per_page=100";

            HttpGet httpGet = new HttpGet(getRequest);
            if (tokenExists)
                httpGet.addHeader("Authorization", "token " + githubToken);
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);
            log.info(String.valueOf(httpResponse.getStatusLine()));
            log.info(String.valueOf(httpGet.getURI()));
            if (httpResponse.getStatusLine().getStatusCode() == 200) {
                JsonNode jsonMap = mapper.readTree(httpResponse.getEntity().getContent());
                jsonMap.forEach(
                        issue -> {
                            List issueInfo = new ArrayList<>();
                            issueInfo.add(issue.get("number").asText());
                            issueInfo.add(
                                    "=HYPERLINK(\""
                                            + issue.get("html_url").asText()
                                            + "\",\""
                                            + issue.get("title").asText().replaceAll("\"", "\"\"")
                                            + "\")");
                            issueInfo.add(issue.get("user").get("login").asText());
                            issueInfo.add(issue.get("state").asText());
                            try {
                                issueInfo.add(
                                        "=HYPERLINK(\" "
                                                + issue.get("milestone").get("html_url").asText().replaceAll("\"", "\"\"")
                                                + "\",\""
                                                + issue.get("milestone").get("title").asText()
                                                + "\")");

                            } catch (NullPointerException ne) {
                                issueInfo.add(" ");
                            }

                            try {
                                JsonNode assignees = issue.get("assignees");
                                StringBuilder assigneeNames = new StringBuilder();
                                assignees.forEach(
                                        assignee -> assigneeNames.append(assignee.get("login").textValue()).append(", "));
                                if (assigneeNames.length() >= 2)
                                    assigneeNames.delete(assigneeNames.length() - 2, assigneeNames.length() - 1);
                                issueInfo.add(assigneeNames.toString());
                            } catch (NullPointerException ne) {
                                issueInfo.add(" ");
                            }
                            try {
                                JsonNode labels = issue.get("labels");
                                StringBuilder labelsList = new StringBuilder();
                                labels.forEach(
                                        assignee -> labelsList.append(assignee.get("name").textValue()).append(", "));
                                if (labelsList.length() >= 2)
                                    labelsList.delete(labelsList.length() - 2, labelsList.length() - 1);
                                issueInfo.add(labelsList.toString());
                            } catch (NullPointerException ne) {
                                issueInfo.add(" ");
                            }
                            String time = issue.get("created_at").asText();
                            time = time.replace("T", ", ");
                            time = time.replace("Z", "");
                            issueInfo.add(time);
                            String closedAt = issue.get("closed_at").asText();
                            if (!closedAt.equals("null")) {
                                closedAt = closedAt.replace("T", ", ");
                                closedAt = closedAt.replace("Z", "");
                                issueInfo.add(closedAt);
                            } else issueInfo.add(" ");

                            issueInfo.add(" "); // Reopen Count
                            issues.add(issueInfo);

                        });


            } else {
                log.error("Bulk upload failed. Couldn't fetch data from GitHub. Please try again later.\n Please provide github token if repo is private. this might cause bulk upload to fail.");
                return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body("Bulk upload failed. Couldn't fetch data from GitHub. Please try again later.\n Please provide github token if repo is private. this might cause bulk upload to fail.");
            }
        }
        //sheetsQuickstart.readOnly();
        sheetsQuickstart.bulkUpdate(spreadsheetId, issues, sheetName);
        return ResponseEntity.ok("All issues from specified repository added to specified sheet successfully");
    }
}
