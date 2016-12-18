package edu.vandy.mooc.aad_3_assg_1.framework.Interfaces;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import edu.vandy.mooc.aad_3_assg_1.framework.orm.Entry;

/**
 * Interface to allow communication between RecyclerView Fragment and Activity.
 */

public interface UpdateEntriesInterface {

    /**
     * Displays the specified list of entries in the recycler view.
     * @param entries List of entries to display.
     */
    void updateEntries(List<Entry> entries);

    /**
     * Returns the list of currently displayed enteries.
     * @return An array list of entries (possibly empty).
     */
    @NonNull ArrayList<Entry> getEntries();
}
