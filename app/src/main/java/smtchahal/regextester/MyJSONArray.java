package smtchahal.regextester;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class MyJSONArray extends JSONArray {

    public MyJSONArray(String s) throws JSONException {
        super(s);
    }

    public MyJSONArray() {
        super();
    }

    /**
     * This method does not override its {@link JSONArray#remove(int)}
     * for a reason: to support Android APIs below API 19.
     *
     * <p class="caution">Note that this method does NOT work the same way as the
     * {@link JSONArray#remove(int)} method. Instead of making
     * changes to the MyJSONArray object itself,
     * it deletes from the object and returns the new
     * MyJSONArray object with the element removed.</p>
     *
     * @param index The index of the item to remove.
     *
     * @return The new array after removing an element.
     *
     * @throws IndexOutOfBoundsException If index &gt;= {@link #length()}
     */
    public MyJSONArray remove(int index) {
        // If index is less than 0, don't bother removing,
        // return the original JSONArray as is
        if (index < 0) {
            return this;
        }

        MyJSONArray jsonArrayNew = new MyJSONArray();
        for (int i = 0; i < this.length(); i++) {
            if (0 <= i && i < index) {
                jsonArrayNew.put(this.optJSONObject(i));
            } else {
                if (this.optJSONObject(i + 1) != null) {
                    jsonArrayNew.put(this.optJSONObject(i + 1));
                }
            }
        }
        return jsonArrayNew;
    }

    /**
     * Returns the position of the fist {@link JSONObject} having {@code key}.
     * Assumes a {@link MyJSONArray} of {@link JSONObject}s.
     *
     * @param key The key corresponding to the JSONObject whose position is to be returned.
     *
     * @return The position corresponding to the first {@link JSONObject} having {@code key}
     * if {@code key} exists, -1 otherwise.
     *
     * @throws JSONException if the given {@link MyJSONArray} does not consist of
     * {@link JSONObject}s
     */
    public int getIndexOfKey(String key) throws JSONException {
        int i;
        boolean keyExists = false;
        for (i = 0; i < this.length(); i++) {
            JSONObject currJSONObject = this.getJSONObject(i);
            //noinspection ConstantConditions
            keyExists = keyExists || currJSONObject.has(key);
            if (keyExists) {
                break;
            }
        }
        if (!keyExists) i = -1;
        return i;
    }

    public String[] getKeys() {
        String[] keys = new String[this.length()];
        for (int i = 0; i < this.length(); i++) {
            Iterator<String> iterator = this.optJSONObject(i).keys();
            while (iterator.hasNext()) {
                keys[i] = iterator.next();
            }
        }
        return keys;
    }
}
