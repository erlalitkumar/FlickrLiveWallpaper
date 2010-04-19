
package novoda.net;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.text.DecimalFormat;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.entity.BufferedHttpEntity;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.util.Pair;

public class GeoNamesAPI {

    /*
     * Using the GeoNames API establish an approximate location
     * @return Pair<Location, String>(Location, placeName)
     */
    public Pair<Location, String> obtainLocation(LocationManager locationMgr) throws ConnectException {
        Log.d(TAG, "Requesting photo details based on approximate location");
         final Location location = getRecentLocation(locationMgr);
        // return new Pair<Location, String>(location,
        // getNearestPlaceName(df.format(location.getLatitude()),
        // df.format(location.getLongitude()),httpClient));
        Pair<Location, String> pair;
        try{
            pair = new Pair<Location, String>(location, getNearestPlaceName(df.format(location
                    .getLatitude()), df.format(location.getLongitude())));
        }catch(NullPointerException e){
            throw new ConnectException();
        }
        
        return pair;
    }
    
    private Location getRecentLocation(LocationManager locManager) {
        Location location = null;
        for (String provider : locManager.getProviders(true)) {
            location = locManager.getLastKnownLocation(provider);
            if (location != null) {
                break;
            }
        }
        return location;
    }

    private String getNearestPlaceName(String lat, String lon) throws ConnectException {
        HttpEntity entity = null;
        JsonNode array = null;

        try {
            HttpResponse response = webSrvMgr.getHTTPResponse(new URL(
                    "http://ws.geonames.org/findNearbyPlaceNameJSON?lat=" + lat + "&lng=" + lon));

            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                array = handleResponse(response);
                array = array.path("geonames").get(0);
            }
        } catch (IOException e) {
            throw new ConnectException(e.getMessage());
        } finally {
            if (entity != null) {
                try {
                    entity.consumeContent();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return array.path("name").getTextValue();
    }

    private JsonNode handleResponse(HttpResponse response) throws ClientProtocolException,
            IOException {
        BufferedHttpEntity ent = new BufferedHttpEntity(response.getEntity());
        JsonNode array = null;
        try {
            array = mapper.readTree(ent.getContent());
            Log.i(TAG, "JSON: " + array.toString());
        } catch (JsonParseException e) {
            Log.e(TAG, "parsing error: " + e.getMessage());
            try {
                Log.i(TAG, "Avoiding printing out the data incase spacial chars crash logcat");
            } catch (Exception e2) {
                Log.e(TAG, "can't read stream");
            }
        }

        return array;
    }

    private final WebServiceMgr webSrvMgr = new WebServiceMgr();

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DecimalFormat df = new DecimalFormat("#.######");

    private static final String TAG = GeoNamesAPI.class.getSimpleName();

}
