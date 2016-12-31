import java.io.*;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Demonstrates how to update a page using the Confluence 5.5 REST API.
 */

//This program can connect to Confluence and pull back information from a request
public class ConvertWikiMediaToConfluence
{
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
        String fileName = "WikiMediaXmlExport.xml";
        String labelYear = "2014";
        int parentPageId = 34472132;

        ArrayList<WikiEntry> wikiEntryArrayList = new ArrayList<WikiEntry>();

        parseXmlAndPopulateWikiList(fileName, wikiEntryArrayList);

        System.out.println(wikiEntryArrayList);

        for (WikiEntry wikiEntry : wikiEntryArrayList)
        {
            JSONObject newPage = defineConfluencePage(wikiEntry.dateString,
                    wikiEntry.createWikiPageWithMarkup(),
                    labelYear,
                    parentPageId,
                    wikiEntry);

            createConfluencePageViaPost(newPage);
        }


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


    public static void parseXmlAndPopulateWikiList(String fileName, ArrayList<WikiEntry> wikiList) throws IOException, SQLException
    {
        File file = new File(fileName);

        BufferedReader inputStream = null;

        WikiEntry wikiEntry = new WikiEntry();

        try
        {
            inputStream = new BufferedReader(new FileReader(file));
            String line;
            String title = "";
            String dayHighlights = "";
            ArrayList<String> personalArrayList = new ArrayList<String>();
            ArrayList<String> workArrayList = new ArrayList<String>();
            ArrayList<String> rentalPropertyArrayList = new ArrayList<String>();
            ArrayList<String> generalArrayList = new ArrayList<String>();
            ArrayList<String> junkArrayList = new ArrayList<String>();
            String dayBefore;
            String dayAfter;
            String[] lineEntry;


            boolean inPage = false;
            boolean inText = false;
            boolean inPersonal = false;
            boolean inWork = false;
            boolean inRentalProperty = false;
            boolean inGeneral = false;

            wikiEntry = new WikiEntry();

            // read in lines from file
            while ((line = inputStream.readLine()) != null)
            {


                if (line.contains("<page>"))
                {
                    inPage = true;

                    wikiEntry = new WikiEntry();

                }

                if (line.contains("</page"))
                {


                    System.out.println(title);
                    System.out.println(dayHighlights);
                    System.out.println("Work: " + workArrayList);
                    System.out.println("Personal: " + personalArrayList);
                    System.out.println("Rental Property: " + rentalPropertyArrayList);
                    System.out.println("General: " + generalArrayList);
                    System.out.println("Junk: " + junkArrayList);

                    title = "";
                    dayHighlights = "";
                    workArrayList.clear();
                    personalArrayList.clear();
                    rentalPropertyArrayList.clear();
                    generalArrayList.clear();
                    junkArrayList.clear();
                    inPage = false;
                    inText = false;
                    inPersonal = false;
                    inWork = false;
                    inGeneral = false;
                    inRentalProperty = false;
                    lineEntry = new String[0];

                    wikiList.add(wikiEntry);

                    System.out.println(wikiEntry.createWikiPageWithMarkup());


                    System.out.println(" ");
                }

                if (inPage)
                {
                    if (line.length() == 0)
                    {
                        continue;
                    }


                    if (line.contains("<title>"))
                    {
                        String[] stringArray = line.split(">");
                        title = stringArray[1];
                        String[] stringArray2 = title.split("<");
                        title = stringArray2[0];

                        wikiEntry.dateString = title;


                    }

                    if (line.contains("<text"))
                    {
                        inText = true;
                        String[] dateStuff = line.split("- ");
                        //String[] dateStuff = line.split(">");
                        dayHighlights = dateStuff[1];

                        wikiEntry.dayInfo = dayHighlights;

                        inGeneral = true;
                        //inPersonal = true;

                    }

                    if (line.contains("</text>"))
                    {
                        inText = false;
                        continue;

                    }

                    if (line.contains("comment>")) {
                        continue;
                    }

                    if (inText == true)
                    {

                        if (inGeneral == true)
                        {
                            if (line.startsWith("**"))
                            {
                                lineEntry = line.split("\\*\\*");
                                generalArrayList.add(lineEntry[1]);
                                wikiEntry.generalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("*"))
                            {
                                lineEntry = line.split("\\*");
                                generalArrayList.add(lineEntry[1]);
                                wikiEntry.generalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("==="))
                            {
                                lineEntry = line.split("===");
                                generalArrayList.add(lineEntry[1]);
                                wikiEntry.generalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("=="))
                            {
//                                lineEntry = line.split("==");
//                                generalArrayList.add(lineEntry[1]);
//                                wikiEntry.generalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith(":"))
                            {
                                lineEntry = line.split(":");
                                generalArrayList.add(lineEntry[1]);
                                wikiEntry.generalArrayList.add(lineEntry[1]);
                            }
                            else
                            {
                                junkArrayList.add(line);
                            }


                        }

                        if (inPersonal == true)
                        {
                            if (line.startsWith("**"))
                            {
                                lineEntry = line.split("\\*\\*");
                                personalArrayList.add(lineEntry[1]);
                                wikiEntry.personalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("*"))
                            {
                                lineEntry = line.split("\\*");
                                personalArrayList.add(lineEntry[1]);
                                wikiEntry.personalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("==="))
                            {
                                lineEntry = line.split("===");
                                personalArrayList.add(lineEntry[1]);
                                wikiEntry.personalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("=="))
                            {
//                                lineEntry = line.split("==");
//                                personalArrayList.add(lineEntry[1]);
//                                wikiEntry.personalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith(":"))
                            {
                                lineEntry = line.split(":");
                                personalArrayList.add(lineEntry[1]);
                                wikiEntry.personalArrayList.add(lineEntry[1]);
                            }
                            else
                            {
                                personalArrayList.add(line);
                                wikiEntry.personalArrayList.add(line);
                            }


                        }

                        if (inWork == true)
                        {

                            if (line.startsWith("**"))
                            {
                                lineEntry = line.split("\\*\\*");
                                workArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("*"))
                            {
                                lineEntry = line.split("\\*");
                                workArrayList.add(lineEntry[1]);
                                wikiEntry.workArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("==="))
                            {
                                lineEntry = line.split("===");
                                workArrayList.add(lineEntry[1]);
                                wikiEntry.workArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("=="))
                            {
//                                lineEntry = line.split("==");
//                                workArrayList.add(lineEntry[1]);
//                                wikiEntry.workArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith(":"))
                            {
                                lineEntry = line.split(":");
                                workArrayList.add(lineEntry[1]);
                                wikiEntry.workArrayList.add(lineEntry[1]);
                            }
                            else
                            {
                                workArrayList.add(line);
                                wikiEntry.workArrayList.add(line);
                            }
                        }

                        if (inRentalProperty == true)
                        {
                            if (line.startsWith("**"))
                            {
                                lineEntry = line.split("\\*\\*");
                                rentalPropertyArrayList.add(lineEntry[1]);
                                wikiEntry.rentalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("*"))
                            {
                                lineEntry = line.split("\\*");
                                rentalPropertyArrayList.add(lineEntry[1]);
                                wikiEntry.rentalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("==="))
                            {
                                lineEntry = line.split("===");
                                rentalPropertyArrayList.add(lineEntry[1]);
                                wikiEntry.rentalArrayList.add(lineEntry[1]);
                            }
                            else if (line.startsWith("=="))
                            {
//                                lineEntry = line.split("==");
//                                rentalPropertyArrayList.add(lineEntry[1]);
//                                wikiEntry.rentalArrayList.add(lineEntry[1]);

                            } else if (line.startsWith(":"))
                            {
                                lineEntry = line.split(":");

                                if (lineEntry.length > 1)
                                {
                                    rentalPropertyArrayList.add(lineEntry[1]);
                                    wikiEntry.rentalArrayList.add(lineEntry[1]);
                                }
                            }
                            else
                            {
                                rentalPropertyArrayList.add(line);
                                wikiEntry.rentalArrayList.add(line);
                            }

                        }

                        if (line.contains("==Personal==")) {
                            inGeneral = false;
                            inWork = false;
                            inRentalProperty = false;
                            inPersonal = true;
                        }

                        if (line.contains("==Work==")) {
                            inGeneral = false;
                            inPersonal = false;
                            inRentalProperty = false;
                            inWork = true;
                        }

                        if (line.contains("==Rental Property==")) {
                            inGeneral = false;
                            inPersonal = false;
                            inRentalProperty = true;
                            inWork = false;
                        }

                        if (line.contains("==") &&
                                !line.contains("==Rental Property==") &&
                                !line.contains("==Work==") &&
                                !line.contains("==Personal==")) {
                            inGeneral = true;
                            inPersonal = false;
                            inRentalProperty = false;
                            inWork = false;
                        }








                    }//End InText

                }


                //{
                //String dateOfEntry = inputStream.readLine();

                //System.out.println(line);

                // }

            }// end while loop



        }//end try
        catch (Exception e)
        {
            e.printStackTrace();
            System.out.println(wikiEntry);
        }// end catch


        //System.out.println(file.getName());

        //file.close();





        //System.out.println("This is a program");
    }

}