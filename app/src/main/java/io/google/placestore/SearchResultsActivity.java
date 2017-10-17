package io.google.placestore;

import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.MenuInflater;
import android.view.View;
import android.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.algolia.instantsearch.ui.views.Hits;
import com.algolia.instantsearch.utils.ItemClickSupport;

public class SearchResultsActivity extends Activity {
    
    public final static String EXTRA_MESSAGE = "io.google.placestore.MESSAGE";
    private Searcher mSearcher;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search_result);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.hitRecycler);
        Hits hits = (Hits) recyclerView;
        hits.setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, int position, View v) {
                String chosenString = ((TextView)v.findViewById(R.id.placeId)).getText().toString();
                Intent startIntent = new Intent(SearchResultsActivity.this, MapsActivity.class);
                startIntent.putExtra(EXTRA_MESSAGE, chosenString);
                startActivity(startIntent);
            }
        });
        handleIntent(getIntent());
        mSearcher = Searcher.create("HSCD77G6HK", "7f34fc74ed47b1c57110cdb1ba5eabde","place");
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }
    
    private void handleIntent(Intent intent) {
        
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_result, menu);
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        ComponentName componentName = getComponentName();
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(componentName);
        MenuItem foundItem = menu.findItem(R.id.action_search_result);
        SearchView searchView = (SearchView) foundItem.getActionView();
        searchView.setSearchableInfo(searchableInfo);
        foundItem.expandActionView();
        InstantSearch instantSearch = new InstantSearch(this, menu, R.id.action_search_result, mSearcher);
        instantSearch.setSearchOnEmptyString(false);
        return true;
    }
}
