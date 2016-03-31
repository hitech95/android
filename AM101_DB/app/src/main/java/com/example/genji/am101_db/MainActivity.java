package com.example.genji.am101_db;

import android.app.FragmentManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    // json array response url
    // private String urlDB = "192.168.1.2";

    private RecyclerView mRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private ProductAdapter pAdapter;
    // lists for products
    List<Product> products;
    // CRUD list for remote DB
    List<Product> productsInserted;
    List<Integer> productsUpdated; // positions
    List<Long> productsDeleted; //ids



    // manager for remote actions
    private Connector mConnector;

    // fragment manager fro dialgs
    FragmentManager fm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // fragment manager for dialogs
        fm = getFragmentManager();

        // create a connector
        mConnector = new Connector(this);

        // as in android developers
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // add an offline set of products
        products = MyData.createList();
        productsInserted = new ArrayList<>();
        productsUpdated = new ArrayList<>();
        productsDeleted = new ArrayList<>();
        // adapter and recycler veiw
        pAdapter = new ProductAdapter(products, this);
        pAdapter.setOnItemClickListener(new ProductAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String name = products.get(position).getName();
                Toast.makeText(MainActivity.this, "#" + position + " - " + name, Toast.LENGTH_SHORT).show();
                MainActivity.this.openUpdateDialog(name, position);
            }
        });
        mRecyclerView.setAdapter(pAdapter);

        // FAB
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                 Snackbar.make(view, "adding a product", Snackbar.LENGTH_LONG).show();
                       // .setAction("Action", null).show();
                MainActivity.this.openInsertDialog();

            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        // drawer.setDrawerListener(toggle); (deprecated)
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id){
            case R.id.drawer_connection:
                testConnection();
                break;
            case R.id.drawer_download:
                downloadAll();
                break;
            case R.id.drawer_upload:
                uploadAll();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // ********************* MY ADDED METHODS *******************

    public void add(Product product){
        productsInserted.add(product);
        pAdapter.add(product, products.size());
    }

    public void update(int position, String description){
        if (!productsUpdated.contains(position)) productsUpdated.add(position);
        pAdapter.update(position, description);
    }

    public void delete(int position){
        productsDeleted.add(products.get(position).getId());
        pAdapter.remove(position);
    }

    public void openInsertDialog(){
        // Create an instance of the dialog fragment and show it
        InsertDialog dialog = new InsertDialog();
        dialog.show(fm, "Insert");
    }

    public void openUpdateDialog(String name, int position){
        // Create an instance of the dialog fragment and show it;

        UpdateDialog dialog = new UpdateDialog();
        Bundle args = new Bundle();
        args.putString("name", name);
        args.putInt("position", position);
        dialog.setArguments(args);
        dialog.show(fm, "");
    }

    // Connector methods

    public void testConnection(){
        mConnector.testConnection();
    }

    public void downloadAll(){
        for(int position = 0; position < products.size(); position++){
            pAdapter.remove(position);
        }
        for(Product product : mConnector.downloadAll()){
            add(product);
        }
        // ***************** NB: notifyAll doesnt work here *****************
    }

    public void uploadAll(){
        // CRUD
        for(Product product : productsInserted) mConnector.insert(product);
        productsInserted.clear();
        Toast.makeText(this, "products inserted in remote db", Toast.LENGTH_SHORT).show();
        for(int position : productsUpdated) mConnector.update(position);
        productsUpdated.clear();
        Toast.makeText(this, "products updated in remote db", Toast.LENGTH_SHORT).show();
        for(long id : productsDeleted) mConnector.delete(id);
        productsDeleted.clear();
        Toast.makeText(this, "products deleted in remote db", Toast.LENGTH_SHORT).show();
    }

}