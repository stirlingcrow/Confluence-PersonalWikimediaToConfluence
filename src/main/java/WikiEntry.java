import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by stirlingcrow on 12/19/16.
 */
public class WikiEntry {


    public String dateString;
    public String dayInfo;
    public Date dateOfEntry;
    public ArrayList<String> workArrayList;
    public ArrayList<String> personalArrayList;
    public ArrayList<String> rentalArrayList;
    public ArrayList<String> generalArrayList;

    public WikiEntry()
    {
        dateString = "";
        dayInfo = "";
        dateOfEntry = null;
        workArrayList = new ArrayList<String>();
        personalArrayList = new ArrayList<String>();
        rentalArrayList = new ArrayList<String>();
        generalArrayList = new ArrayList<String>();
    }

    public String createWikiPageWithMarkup()
    {
        String wikiPage = "";

        //Create header
        String beforeDateHeader = "<p><a href=\"/wiki/display/JOUR/" + this.getDayBefore() + "\">" + this.getDayBefore() + "</a>";
        String breaker = " | ";
        String afterDateHeader = "<a href=\"/wiki/display/JOUR/" + this.getDayAfter() + "\">" + this.getDayAfter() + "</a>";
        String dayInfoEnd = " - " + dayInfo + "</p>";

        String wikiPageHeader = beforeDateHeader + breaker + afterDateHeader + dayInfoEnd;
        wikiPage = wikiPage + wikiPageHeader;

        boolean personalFlag = false;
        if (personalArrayList.size() > 0)
        {
            String personalHeader = "<h1 id=\"id-" + dateString + "-Personal\">Personal</h1>";

            wikiPage = wikiPage + personalHeader;
            wikiPage = wikiPage + getPersonalItemsWithMarkup();
            personalFlag = true;
        }

        if (generalArrayList.size() > 0)
        {
            if (personalFlag == false)
            {
                String personalHeader = "<h1 id=\"id-" + dateString + "-Personal\">Personal</h1>";
                wikiPage = wikiPage + personalHeader;
            }

            wikiPage = wikiPage + getGeneralItemsWithMarkup();
        }

        if (workArrayList.size() > 0)
        {
            String workPropertyHeader = "<h1 id=\"id-" + dateString + "-Work\">Work</h1>";

            wikiPage = wikiPage + workPropertyHeader;
            wikiPage = wikiPage + getWorkItemsWithMarkup();
        }

        if (rentalArrayList.size() > 0)
        {
            String rentalPropertyHeader = "<h1 id=\"id-" + dateString + "-Rental Property\">Rental Property</h1>";

            wikiPage = wikiPage + rentalPropertyHeader;
            wikiPage = wikiPage + getRentalPropertyItemsWithMarkup();
        }

        return wikiPage;
    }

    public String getDayBefore()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dayBefore = "";

        try
        {
            Date date = formatter.parse(dateString);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.DATE, -1);
            dayBefore = formatter.format(c.getTime());

        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return dayBefore;
    }

    public String getDayAfter()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dayBefore = "";

        try
        {
            Date date = formatter.parse(dateString);
            Calendar c = Calendar.getInstance();
            c.setTime(date);
            c.add(Calendar.DATE, 1);
            dayBefore = formatter.format(c.getTime());

        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        return dayBefore;
    }

    public String getRentalPropertyItemsWithMarkup()
    {
        String stringItems = "<ul>";

        for (String infoItem : rentalArrayList)
        {
            stringItems = stringItems + "<li>" + infoItem + "</li>";
        }

        stringItems = stringItems + "</ul>";

        return stringItems;
    }

    public String getGeneralItemsWithMarkup()
    {
        String stringItems = "<ul>";

        for (String infoItem : generalArrayList)
        {
            stringItems = stringItems + "<li>" + infoItem + "</li>";
        }

        stringItems = stringItems + "</ul>";

        return stringItems;
    }

    public String getPersonalItemsWithMarkup()
    {
        String stringItems = "<ul>";

        for (String infoItem : personalArrayList)
        {
            stringItems = stringItems + "<li>" + infoItem + "</li>";
        }

        stringItems = stringItems + "</ul>";

        return stringItems;
    }

    public String getWorkItemsWithMarkup()
    {
        String stringItems = "<ul>";

        for (String infoItem : workArrayList)
        {
            stringItems = stringItems + "<li>" + infoItem + "</li>";
        }

        stringItems = stringItems + "</ul>";

        return stringItems;
    }

    public void populateDayInfo()
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String dayOfWeek = "";

        try
        {
            Date date = formatter.parse(dateString);
            //Calendar c = Calendar.getInstance();
            //c.setTime(date);

            formatter.applyPattern("EEEE");
            dayOfWeek = formatter.format(date) + ".";



        } catch (ParseException e)
        {
            e.printStackTrace();
        }

        dayInfo = dayOfWeek;


    }

    @Override
    public String toString() {
        return dateString + " - " + dayInfo;
    }
}
