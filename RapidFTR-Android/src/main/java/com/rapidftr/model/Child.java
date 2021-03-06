package com.rapidftr.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.rapidftr.RapidFtrApplication;
import com.rapidftr.utils.RapidFtrDateTime;
import lombok.Getter;
import lombok.Setter;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static com.rapidftr.database.Database.ChildTableColumn;
import static com.rapidftr.database.Database.ChildTableColumn.*;
import static com.rapidftr.model.Child.History.*;
import static com.rapidftr.utils.JSONArrays.asJSONArray;
import static com.rapidftr.utils.JSONArrays.asList;

public class Child extends JSONObject implements Parcelable {

    public static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    public static final String EMPTY_STRING = "";

    protected
    @Getter
    @Setter
    boolean synced;

    public Child() {
        try {
            setSynced(false);
            setCreatedAt(RapidFtrDateTime.now().defaultFormat());
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Child(Parcel parcel) throws JSONException {
        this(parcel.readString());
    }

    public Child(String id, String owner, String content) throws JSONException {
        this(content);
        setUniqueId(id);
        setOwner(owner);
    }

    public Child(String id, String owner, String content, boolean synced) throws JSONException {
        this(id, owner, content);
        setSynced(synced);
    }

    public Child(String content) throws JSONException {
        super(Strings.nullToEmpty(content).trim().length() == 0 ? "{}" : content);
        setHistories();
        if (!has(created_at.getColumnName())) {
            setCreatedAt(RapidFtrDateTime.now().defaultFormat());
        }
    }

    private void setHistories() throws JSONException {
        String histories = this.optString(HISTORIES, null);
        if (histories != null)
            this.put(HISTORIES, new JSONArray(histories));
    }

    public Child(String content, boolean synced) throws JSONException {
        this(content);
        setSynced(synced);
    }

    public String getString(String key) {
        try {
            return super.getString(key);
        } catch (JSONException e) {
            Log.e(RapidFtrApplication.APP_IDENTIFIER, e.getMessage());
        }
        return null;
    }

    @Override
    public JSONArray names() {
        JSONArray names = super.names();
        return names == null ? new JSONArray() : names;
    }

    @Override
    public JSONObject put(String key, Object value) {
        if (value != null && value instanceof String) {
            value = Strings.emptyToNull(((String) value).trim());
        } else if (value != null && value instanceof JSONArray && ((JSONArray) value).length() == 0) {
            value = null;
        }
        try {
            return super.put(key, value);
        } catch (JSONException e) {
            Log.e(RapidFtrApplication.APP_IDENTIFIER, e.getMessage());
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeString(getJsonString());
    }

    public String getId() throws JSONException {
        return getString(internal_id.getColumnName());
    }

    public String getUniqueId() throws JSONException {
        return has(unique_identifier.getColumnName()) ? getString(unique_identifier.getColumnName()) : null;
    }

    public String getShortId() throws JSONException {
        if (!has(unique_identifier.getColumnName()))
            return null;

        int length = getUniqueId().length();
        return length > 7 ? getUniqueId().substring(length - 7) : getUniqueId();
    }

    public void setUniqueId(String id) throws JSONException {
        put(unique_identifier.getColumnName(), id);
    }

    public String getOwner() throws JSONException {
        return getString(created_by.getColumnName());
    }

    public void setOwner(String owner) throws JSONException {
        put(created_by.getColumnName(), owner);
    }

    public void setOrganisation(String userOrg) throws JSONException {
        put(created_organisation.getColumnName(), userOrg);
    }

    public String getName() {
        return optString(name.getColumnName(), EMPTY_STRING);
    }

    public void setName(String childName) throws JSONException {
        put(name.getColumnName(), childName);
    }

    public String getCreatedAt() throws JSONException {
        return getString(created_at.getColumnName());
    }

    protected void setCreatedAt(String createdAt) throws JSONException {
        put(created_at.getColumnName(), createdAt);
    }

    public String getLastUpdatedAt() throws JSONException {
        return optString(last_updated_at.getColumnName(), null);
    }

    public void setLastUpdatedAt(String lastUpdatedAt) throws JSONException {
        put(last_updated_at.getColumnName(), lastUpdatedAt);
    }

    public void setLastSyncedAt(String lastSyncedAt) throws JSONException {
        put(last_synced_at.getColumnName(), lastSyncedAt);
    }

    public String getLastSyncedAt() throws JSONException {
        return optString(last_synced_at.getColumnName(), null);
    }

    public String getSyncLog() throws JSONException {
        return optString(syncLog.getColumnName(), null);
    }

    public void setSyncLog(String syncLog1) throws JSONException {
        put(syncLog.getColumnName(), syncLog1);
    }

    public void addToJSONArray(String key, Object element) throws JSONException {
        JSONArray array = has(key) ? getJSONArray(key) : new JSONArray();
        List<Object> list = asList(array);
        if (!list.contains(element))
            list.add(element);

        put(key, asJSONArray(list));
    }

    public void removeFromJSONArray(String key, Object element) throws JSONException {
        if (!has(key))
            return;

        JSONArray array = getJSONArray(key);
        List<Object> list = asList(array);
        list.remove(element);
        put(key, asJSONArray(list));
    }

    public String getJsonString() {
        return toString();
    }

    public void generateUniqueId() throws JSONException {
        if (has(unique_identifier.getColumnName())) {
            /* do nothing */
        } else {
            setUniqueId(createUniqueId());
        }
    }

    protected String createUniqueId() throws JSONException {
        return UUID.randomUUID().toString();
    }

    public boolean isValid() {
        int numberOfNonInternalFields = names().length();

        for (ChildTableColumn field : ChildTableColumn.internalFields()) {
            if (has(field.getColumnName())) {
                numberOfNonInternalFields--;
            }
        }
        return numberOfNonInternalFields > 0;
    }

    public boolean equals(Object other) {
        try {
            return (other != null && other instanceof JSONObject) && JSON_MAPPER.readTree(toString()).equals(JSON_MAPPER.readTree(other.toString()));
        } catch (IOException e) {
            return false;
        }
    }

    public JSONObject values() throws JSONException {
        List<Object> names = asList(names());
        Iterable<Object> systemFields = Iterables.transform(ChildTableColumn.systemFields(), new Function<ChildTableColumn, Object>() {
            @Override
            public Object apply(ChildTableColumn childTableColumn) {
                return childTableColumn.getColumnName();
            }
        });

        Iterables.removeAll(names, Lists.newArrayList(systemFields));
        return new JSONObject(this, names.toArray(new String[names.size()]));
    }

    public List<History> changeLogs(Child child, JSONArray existingHistories) throws JSONException {
        //fetch all the histories from this child, which are greater than last_synced_at and merge the histories
        JSONArray names = this.names();
        List<History> histories = getHistoriesFromJsonArray(existingHistories);
        try {
            if (!child.optString("last_synced_at").equals("")) {
                Calendar lastSync = RapidFtrDateTime.getDateTime(child.optString("last_synced_at"));
                for (History history : histories) {
                    Calendar lastSavedAt = RapidFtrDateTime.getDateTime((String) history.get("datetime"));
                    if (lastSavedAt.after(lastSync)) {
                        JSONObject changes = (JSONObject) history.get("changes");
                        for (int i = 0; i < names.length(); i++) {
                            String newValue = this.optString(names.getString(i), "");
                            String oldValue = child.optString(names.getString(i), "");
                            if (!oldValue.equals(newValue)) {
                                JSONObject fromTo = new JSONObject();
                                fromTo.put(FROM, oldValue);
                                fromTo.put(TO, newValue);
                                changes.put(names.getString(i), fromTo);
                            }
                        }
                        history.put(USER_NAME, child.getOwner());
                        history.put(USER_ORGANISATION, child.optString("created_organisation"));
                        history.put(DATETIME, RapidFtrDateTime.now().defaultFormat());
                        break;
                    }
                }
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        return histories;
    }

    private List<History> getHistoriesFromJsonArray(JSONArray histories) throws JSONException {
        List<Object> objects = histories != null ? asList(histories) : new ArrayList<Object>();
        List<History> childHistories = new ArrayList<History>();
        for (Object object : objects) {
            childHistories.add(new History(object.toString()));
        }
        return childHistories;
    }

    public boolean isNew() {
        return !has(internal_id.getColumnName());
    }

    public JSONArray getPhotos() {
        JSONArray photo_keys = new JSONArray();
        try {
            photo_keys = getJSONArray("photo_keys");
        } catch (JSONException e) {
            Log.e("Fetching photos", "photo_keys field is available");
        }
        return photo_keys;
    }

    public class History extends JSONObject implements Parcelable {
        public static final String HISTORIES = "histories";
        public static final String USER_NAME = "user_name";
        public static final String USER_ORGANISATION = "user_organisation";
        public static final String DATETIME = "datetime";
        public static final String CHANGES = "changes";
        public static final String FROM = "from";
        public static final String TO = "to";

        public History(String jsonSource) throws JSONException {
            super(jsonSource);
        }

        public History() {

        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(this.toString());
        }

    }
}
