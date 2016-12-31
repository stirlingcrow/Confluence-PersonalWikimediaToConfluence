import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;


public class CreateJournalYearPage {

    //private static final String BASE_URL = "http://localhost:1990/confluence";
    private static final String BASE_URL = "https://<context>.atlassian.net/wiki";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String ENCODING = "utf-8";

    public static String createContentRestUrl()throws UnsupportedEncodingException
    {
        return String.format("%s/rest/api/content/?&os_authType=basic&os_username=%s&os_password=%s", BASE_URL, URLEncoder.encode(USERNAME, ENCODING), URLEncoder.encode(PASSWORD, ENCODING));

    }

    public static void main(final String[] args) throws Exception
    {
        int yearToCreate = 2016;

        //Run this one first
        int parentPageId = 9994246;
        createParentYearPage(yearToCreate, parentPageId);

        //Run this one afterwards (you'll need to get the ID of the page you just created)
        //int parentPageId = 9994246;
        //createChildPagesForYear(yearToCreate, parentPageId);



    }

    public static void createChildPagesForYear(int yearInJournal, int parentPageId) throws Exception
    {
        ArrayList<WikiEntry> wikiEntryList = new ArrayList<WikiEntry>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = "";

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.YEAR, yearInJournal);

        String endDate = Integer.toString(yearInJournal) + "-12-31";

        while (!dateString.equals(endDate))
        {
            dateString = formatter.format(c.getTime());

            WikiEntry wikiEntry = new WikiEntry();

            wikiEntry.dateString = dateString;

            //Populating the day information...
            wikiEntry.populateDayInfo();



            wikiEntryList.add(wikiEntry);

            //Increment the day by one...
            c.add(Calendar.DATE, 1);
        }


        System.out.println("Got here.");

        for (WikiEntry wikiEntry : wikiEntryList)
        {
            String wikiPage = wikiEntry.createWikiPageWithMarkup();

            //Adding the Personal Header
            wikiPage = wikiPage + "<h1 id=\"id-" + wikiEntry.dateString + "-Personal\">Personal</h1>";

            JSONObject newPage = defineConfluencePage(wikiEntry.dateString ,
                    wikiPage,
                    Integer.toString(yearInJournal),
                    parentPageId,
                    wikiEntry);

            createConfluencePageViaPost(newPage);
        }


    }

    public static void createParentYearPage(int yearInJournal, int parentPageId) throws Exception
    {
        ArrayList<String> dateList = new ArrayList<String>();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = "";

        Calendar c = Calendar.getInstance();
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.YEAR, yearInJournal);

        String endDate = Integer.toString(yearInJournal) + "-12-31";

        while (!dateString.equals(endDate))
        {
            dateString = formatter.format(c.getTime());

            dateList.add(dateString);

            System.out.println(dateString);

            //Increment the day by one...
            c.add(Calendar.DATE, 1);
        }



        //I like the most recent to show up at the top
        Collections.reverse(dateList);

        String wikiPage = "<ul>";

        for (String dateEntry : dateList)
        {
            String hrefAndDate = "<li><a href=\"/wiki/display/JOUR/" + dateEntry + "\">" + dateEntry + "</a></li>";
            wikiPage = wikiPage + hrefAndDate;
        }

        wikiPage = wikiPage + "</ul>";

        String wikiPageTitle = Integer.toString(yearInJournal) + " Journal";
        WikiEntry wikiEntry = new WikiEntry();

        JSONObject newPage = defineConfluencePage(wikiPageTitle ,
                wikiPage,
                Integer.toString(yearInJournal),
                parentPageId,
                wikiEntry);

        createConfluencePageViaPost(newPage);
    }

    public static void createConfluencePageViaPost(JSONObject newPage) throws Exception
    {
        HttpClient client = new DefaultHttpClient();

        // Send update request
        HttpEntity putPageEntity = null;


        try
        {

            HttpPost postPageRequest = new HttpPost(createContentRestUrl());

            StringEntity entity = new StringEntity(newPage.toString(), ContentType.APPLICATION_JSON);
            postPageRequest.setEntity(entity);

            HttpResponse putPageResponse = client.execute(postPageRequest);
            putPageEntity = putPageResponse.getEntity();

            System.out.println("Post Page Request returned " + putPageResponse.getStatusLine().toString());
            System.out.println("");
            System.out.println(IOUtils.toString(putPageEntity.getContent()));
        }
        finally
        {
            EntityUtils.consume(putPageEntity);
        }
    }

    public static JSONObject defineConfluencePage(String dateString,
                                                  String wikiEntryText,
                                                  String labelYear,
                                                  int parentPageId,
                                                  WikiEntry wikiEntry) throws JSONException
    {
        //This would be the command in Python (similar to the example
        //in the Confluence example:
        //
        //curl -u admin:admin -X POST -H 'Content-Type: application/json' -d'{
        // "type":"page",
        // "title":"new page",
        // "ancestors":[{"id":456}],
        // "space":{"key":"JOUR"},
        // "body":
        //        {"storage":
        //                   {"value":"<p>This is a new page</p>",
        //                    "representation":"storage"}
        //        },
        // "metadata":
        //             {"labels":[
        //                        {"prefix":"global",
        //                        "name":"journal"},
        //                        {"prefix":"global",
        //                        "name":"2016_journal"}
        //                       ]
        //             }
        // }'
        // http://localhost:8080/confluence/rest/api/content/ | python -mjson.tool

        JSONObject newPage = new JSONObject();

        // "type":"page",
        // "title":"new page"
        newPage.put("type","page");
        newPage.put("title", dateString);

        // "ancestors":[{"id":33947677}],
        JSONObject parentPage = new JSONObject();
        parentPage.put("id",parentPageId);

        JSONArray parentPageArray = new JSONArray();
        parentPageArray.put(parentPage);

        newPage.put("ancestors", parentPageArray);

        // "space":{"key":"JOUR"},
        JSONObject spaceOb = new JSONObject();
        spaceOb.put("key","JOUR");
        newPage.put("space", spaceOb);

        // "body":
        //        {"storage":
        //                   {"value":"<p>This is a new page</p>",
        //                    "representation":"storage"}
        //        },
        JSONObject jsonObjects = new JSONObject();

        jsonObjects.put("value", wikiEntryText);
        jsonObjects.put("representation","storage");

        JSONObject storageObject = new JSONObject();
        storageObject.put("storage", jsonObjects);

        newPage.put("body", storageObject);


        //LABELS
        // "metadata":
        //             {"labels":[
        //                        {"prefix":"global",
        //                        "name":"journal"},
        //                        {"prefix":"global",
        //                        "name":"2016_journal"}
        //                       ]
        //             }
        JSONObject prefixJsonObject1 = new JSONObject();
        prefixJsonObject1.put("prefix","global");
        prefixJsonObject1.put("name","journal");
        JSONObject prefixJsonObject2 = new JSONObject();
        prefixJsonObject2.put("prefix","global");

        String journalLabelYear = labelYear + "_journal";
        prefixJsonObject2.put("name",journalLabelYear);

        JSONArray prefixArray = new JSONArray();
        prefixArray.put(prefixJsonObject1);
        prefixArray.put(prefixJsonObject2);

        //Adding the rental_property label if there is rental property info..
        if (wikiEntry.rentalArrayList.size() > 0)
        {
            JSONObject prefixJsonObject3 = new JSONObject();
            prefixJsonObject3.put("prefix","global");
            prefixJsonObject3.put("name","rental_property");

            prefixArray.put(prefixJsonObject3);
        }

        JSONObject labelsObject = new JSONObject();
        labelsObject.put("labels", prefixArray);

        newPage.put("metadata",labelsObject);

        return newPage;
    }



}
