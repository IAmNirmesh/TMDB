package com.android.nirmesh.tmdb;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.nirmesh.tmdb.adapter.MoviesAdapter;
import com.android.nirmesh.tmdb.api.Client;
import com.android.nirmesh.tmdb.api.Service;
import com.android.nirmesh.tmdb.model.Movie;
import com.android.nirmesh.tmdb.model.MoviesResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TMDBActivity extends AppCompatActivity {

    private RecyclerView recycler_view;
    private MoviesAdapter moviesAdapter;
    private List<Movie> movieList;
    ProgressDialog mProgressDialog;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    public static final String LOG_TAG = MoviesAdapter.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tmdb);

        initViews();

        mSwipeRefreshLayout = findViewById(R.id.main_content);
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_orange_dark);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh(){
                initViews();
                Toast.makeText(TMDBActivity.this, "Movies Refreshed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Activity getActivity() {
        Context context = this;
        while (context instanceof ContextWrapper){
            if (context instanceof Activity){
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private void initViews() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Fetching movies...");
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        recycler_view = findViewById(R.id.recycler_view);

        movieList = new ArrayList<>();
        moviesAdapter = new MoviesAdapter(this, movieList);

        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            recycler_view.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recycler_view.setLayoutManager(new GridLayoutManager(this, 4));
        }

        recycler_view.setItemAnimator(new DefaultItemAnimator());
        recycler_view.setAdapter(moviesAdapter);
        moviesAdapter.notifyDataSetChanged();

        loadJSON();
    }

    private void loadJSON() {
        try {
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please obtain API Key firstly from themoviedb.org", Toast.LENGTH_SHORT).show();
                mProgressDialog.dismiss();
                return;
            }

            Client Client = new Client();
            Service apiService = Client.getClient().create(Service.class);

            Call<MoviesResponse> call = apiService.getPopularMovies(BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<MoviesResponse>() {
                @Override
                public void onResponse(Call<MoviesResponse> call, Response<MoviesResponse> response) {
                    List<Movie> movies = response.body().getResults();
                    recycler_view.setAdapter(new MoviesAdapter(getApplicationContext(), movies));
                    recycler_view.smoothScrollToPosition(0);

                    if (mSwipeRefreshLayout.isRefreshing()){
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    mProgressDialog.dismiss();
                }

                @Override
                public void onFailure(Call<MoviesResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(TMDBActivity.this, "Error Fetching Data!", Toast.LENGTH_SHORT).show();

                }
            });
        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
