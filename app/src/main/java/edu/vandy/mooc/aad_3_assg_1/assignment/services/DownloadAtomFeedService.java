package edu.vandy.mooc.aad_3_assg_1.assignment.services;

import android.app.Activity;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import edu.vandy.mooc.aad_3_assg_1.assignment.activities.MainActivity;
import edu.vandy.mooc.aad_3_assg_1.framework.net.NetEntry;
import edu.vandy.mooc.aad_3_assg_1.framework.net.YoutubeFeedParser;
import edu.vandy.mooc.aad_3_assg_1.framework.orm.Entry;

/**
 * This service downloads the metadata of the last 15 videos from CNN's YouTube page,
 * converts that information into a List of {@link Entry} objects and returns it via an Intent
 * and Broadcast Receiver.
 */
@SuppressWarnings({"unused", "SameParameterValue"})
public class DownloadAtomFeedService extends IntentService {

    /**
     * Debugging tag used by the Android logger.
     */
    private static final String TAG = DownloadAtomFeedService.class.getCanonicalName();

    /**
     * String constant used to extract the request code.
     */
    private static final String REQUEST_CODE = "REQUEST_CODE";

    /**
     * String constant used to extract the results code.
     */
    private static final String DOWNLOAD_RESULTS = "DOWNLOAD RESULTS";

    /**
     * String constant used to extract the URL to an ATOM Feed from a Bundle.
     */
    private static final String FEED_URL = "FEED_URL";

    /**
     * String constant used to extract the parcelable array of Entry(s) from a Bundle.
     */
    private static final String ENTRY_ARRAY_KEY = "ENTRY_ARRAY_KEY";

    private static final int REQUEST_CODE_FAILED_VALUE = -1;
    /**
     * Constructor that sets the name of the IntentService.
     */
    public DownloadAtomFeedService() {
        super("DownloadAtomFeedService");
    }

    /**
     * Factory method that returns an explicit Intent for downloading Entry(s) from a YouTube Feed.
     */
    public static Intent makeIntent(Context context,
                                    int requestCode,
                                    Uri url) {
        // Create an intent that will call this server to download the Entry(s) 
        // from the web. which involves (1) setting the URL as "data" to the
        // intent, (2) putting the request code as an "extra" to the intent.
        Intent intent = new Intent();
        intent.setClassName(context, DownloadAtomFeedService.class.getName());
        intent.setData(url);
        intent.putExtra(DownloadAtomFeedService.REQUEST_CODE, requestCode);
        return intent;
    }

    /**
     * Helper method to ease in using this class by localizing values needed for extracting data
     * from reply messages. Helps in getting Result Code.
     *
     * @param message message returned from this Service.
     * @return the result code.
     */
    public static int getResultCode(Message message) {
        // Check to see if the download succeeded.
        return message.getData().getInt(DownloadAtomFeedService.DOWNLOAD_RESULTS);
    }

    /**
     * Helper method to ease in using this class by localizing values needed for extracting data
     * from reply messages. Helps in getting Request Uri.
     *
     * @param message message returned from this Service.
     * @return the request Uri.
     */
    public static Uri getRequestUri(Message message) {
        // Extract the data from Message, which is in the form of a
        // Bundle that can be passed across processes.
        Bundle data = message.getData();

        // call getRequestUri(Bundle) on the data bundle and return the Uri it returns.
        return  DownloadAtomFeedService.getRequestUri(data);
    }

    public static Uri getRequestUri(Bundle data) {
        // use 'FEED_URL' to extract the string representation of the Uri from the Message
        String url = data.getString(DownloadAtomFeedService.FEED_URL);

        // Parse the String of the url to get a Uri and return it.
        return Uri.parse(url);
    }


    /**
     * Helper method to ease in using this class by localizing values needed for extracting data
     * from reply messages. Helps in getting Request Code.
     *
     * @param message message returned from this Service.
     * @return the request code.
     */
    public static int getRequestCode(Message message) {
        // Extract the data from Message, which is in the form of a
        // Bundle that can be passed across processes.
        Bundle data = message.getData();

        // Extract the request code and return it.
        return data.getInt(DownloadAtomFeedService.REQUEST_CODE);
    }

    /**
     * Helper method to ease in using this class by localizing values needed for extracting data
     * from reply messages. Helps in getting the Entry(s) from the message.
     *
     * @param message message returned from this Service.
     * @return an ArrayList of the Entry(s) downloaded from the Uri.
     */
    public static ArrayList<Entry> getEntries(@NonNull Message message) {
        // we give you this method since it isn't covered in full detail since it is beyond the
        // scope of this course.
        if (message.getData() == null) {
            return null;
        }
        return getEntries(message.getData());
    }

    /**
     * Helper method to ease in using this class by localizing values needed for extracting data
     * from reply messages. Helps in getting the Entry(s) from the Bundle.
     *
     * @param data Bundle originally returned by this Service, which contains the Entry(s).
     * @return ArrayList of Entry(s)
     */
    public static ArrayList<Entry> getEntries(@NonNull Bundle data) {
        // we give you this method since it isn't covered in full detail since it is beyond the
        // scope of this course.
        return data.getParcelableArrayList(ENTRY_ARRAY_KEY);
    }

    /**
     * Hook method dispatched by the IntentService framework to
     * download the YouTube feed requested via data in an intent,
     * and return the Entry(s) back to the MainActivity via a
     * broadcasted Intent.
     */
    @Override
    public void onHandleIntent(Intent intent) {
        // Get the URL associated with the Intent data.
        Uri url = intent.getData();

        // Download the requested YouTube Atom Feed.
        List<Entry> downloadedAtomFeeds = this.downloadAtomFeed(url.toString());
        Log.i("TRACE", "There are " + downloadedAtomFeeds.size() + " downloaded feeds.");

        // Extract the request code.
        int requestCode = intent.getIntExtra(DownloadAtomFeedService.REQUEST_CODE,
                DownloadAtomFeedService.REQUEST_CODE_FAILED_VALUE);

        // Send the YouTube Atom Feed Entries back to the
        // MainActivity via the sendEntries(...) method.
        this.sendEntries((ArrayList<Entry>) downloadedAtomFeeds, url, requestCode);
    }


    /**
     * Send the pathname back to the MainActivity via the LocalBroadcastManager + an Intent.
     */
    private void sendEntries(ArrayList<Entry> entries,
                             Uri url,
                             int requestCode) {
        // Call the makeReplyBundle(...) method to create a new Bundle containing the results.
        Bundle data = this.makeReplyBundle(entries, url, requestCode);

        // Create a new Intent via calling MainActivity's makeBroadcastReceiverIntent()
        // Then, store the newly created bundle in the Intent via putExtras(...)
        Intent intent = MainActivity.makeBroadcastReceiverIntent();
        intent.putExtras(data);

        // Now we will need to launch the Intent containing the results Bundle.
        // Get an instance of LocalBroadcastManager to send the Broadcast of the Intent via
        // sendBroadcast(...).
        LocalBroadcastManager.getInstance(this.getApplicationContext()).sendBroadcast(intent);
    }

    /**
     * A factory method that creates a Bundle to return to the
     * MainActivity with the list of Entry(s) downloaded.
     */
    private Bundle makeReplyBundle(ArrayList<Entry> entries,
                                   Uri url,
                                   int requestCode) {

        // Create a new Bundle named 'data' to handle the result.
        Bundle data = new Bundle();

        // use 'putParcelableArrayList(...)' to store the ArrayList of Entry(s) in the bundle.
        data.putParcelableArrayList(DownloadAtomFeedService.ENTRY_ARRAY_KEY, entries);

        // Put the requestCode into the Bundle via the REQUEST_CODE key.
        data.putInt(DownloadAtomFeedService.REQUEST_CODE, requestCode);

        // Put the url as a string into the Bundle via the FEED_URL key.
        data.putString(DownloadAtomFeedService.FEED_URL, url.toString());

        // Set a field in the Message to indicate whether the download
        // succeeded or failed.
        if (requestCode == DownloadAtomFeedService.REQUEST_CODE_FAILED_VALUE)
            data.putString("message", "Download was a failure.");
        else {
            data.putString("message", "Download was a success.");
            data.putInt(DownloadAtomFeedService.DOWNLOAD_RESULTS, Activity.RESULT_OK);
        }
        // return the bundle 'data'.
        return data;
    }

    /**
     * Extract the results code from the bundle originally created by this service.
     *
     * @param bundle the bundle originally created by this Service containing the results code
     * @return the download results code
     */
    public static int getDownloadResultsCode(Bundle bundle) {
        return bundle.getInt(DOWNLOAD_RESULTS);
    }

   /*
     * These Helper Methods are provided for you, so that you can focus on implementing the
     * service, and not have to worry about the specifics of downloading and parsing an ATOM feed
     * from YouTube.
     */

    /**
     * Download the YouTube AtomFeed and generate the Entry(s) that were in the Feed.
     *
     * @param feedURL The url of the YouTube feed
     * @return a List of type Entry that were in the Feed url provided.
     */
    private List<Entry> downloadAtomFeed(final String feedURL) {
        android.util.Log.d(TAG, "downloadAtomFeed()");
        final List<Entry> entries = new ArrayList<>();

        // Download Entries
        try {
            // Make sure that this thread has not been interrupted.
            checkHaMeRThreadNotInterupted();
            // download and process the feed from the given url.
            List<NetEntry> netEntries = downloadNetEntries(feedURL);
            Log.d(TAG, "netEntries size = " + netEntries.size());
            List<Entry> newEntries = convertToOrmEntries(netEntries);
            Log.d(TAG, "entries size = " + newEntries.size());
            entries.addAll(newEntries);
            // check that thread wasn't interrupted again before returning results.
            checkHaMeRThreadNotInterupted();
        } catch (Exception ex) {
            Log.e(TAG, "Exception downloading Atom Feed: " + ex.getMessage());
        }

        // Return the List<Entry> return object created above.
        return entries;
    }

    /**
     * Helper method to check if thread has been interrupted & throw exception if it has
     *
     * @throws Exception Thrown Exception notifying that the HaMeR Thread was interrupted.
     */
    private void checkHaMeRThreadNotInterupted() throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            Log.d(TAG, "HaMer thread interrupted");
            throw new Exception("HaMer thread interrupted, halting execution.");
        }
    }

    /**
     * Helper Method to convert from "NetEntry"(s) to "Entry"(s). We have two different POJO
     * (Plain Old Java Object(Classes that do not inherit from anything othe than Object))
     * Classes because this allows for better de-coupling of our internal 'Entry's and external
     * (Web) 'Entry's. This allows us to more easily make changes to either one of the two
     * classes in the future if need be without impacting the other.
     *
     * @param netEntries ArrayList of NetEntry(s) to convert to ArrayList of Entry(s)
     * @return converted ArrayList of Entry(s)
     */
    private List<Entry> convertToOrmEntries(List<NetEntry> netEntries) {
        // Create an empty ArrayList of type "Entry".
        List<Entry> entries = new ArrayList<>();
        // Loop through each 'NetEntry', and convert it to an 'Entry' object.
        for (NetEntry netEntry : netEntries) {
            entries.add(
                    new Entry(netEntry.id,
                            netEntry.title,
                            netEntry.link,
                            netEntry.published,
                            netEntry.author
                    )
            ); // end of 'add'
        } // end of loop through each NetEntry
        // return the ArrayList of newly created 'Entry' Objects.
        return entries;
    }

    /**
     * Download the NetEntries from a provided URL
     *
     * @param urlString the url to grab the ATOM feed from.
     * @return the NetEntrys that were in the ATOM feed.
     * @throws IOException
     * @throws XmlPullParserException
     */
    @SuppressWarnings("ThrowFromFinallyBlock")
    private List<NetEntry> downloadNetEntries(final String urlString)
            throws IOException, XmlPullParserException {

        List<NetEntry> rValues;

        BufferedInputStream in = null;

        try {
            in = new BufferedInputStream(new URL(urlString).openStream());

            YoutubeFeedParser parser = new YoutubeFeedParser();

            rValues = parser.parse(in);
            Log.d(TAG, "downloadNetEntries rValue size = " + rValues.size());


        } finally {
            if (in != null) {
                in.close();
            }
        }
        return rValues;
    }


}
