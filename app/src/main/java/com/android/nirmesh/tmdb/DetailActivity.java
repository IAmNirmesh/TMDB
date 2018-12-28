package com.android.nirmesh.tmdb;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nirmesh.tmdb.adapter.TrailerAdapter;
import com.android.nirmesh.tmdb.api.Client;
import com.android.nirmesh.tmdb.api.Service;
import com.android.nirmesh.tmdb.data.FavoriteDbHelper;
import com.android.nirmesh.tmdb.model.Movie;
import com.android.nirmesh.tmdb.model.Trailer;
import com.android.nirmesh.tmdb.model.TrailerResponse;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DetailActivity extends AppCompatActivity {

    TextView title, plotSynopsis, userRating, releaseDate;
    ImageView thumbnail_image_header;

    private RecyclerView recycler_view_trailer;
    private TrailerAdapter adapter;
    private List<Trailer> trailerList;

    private FavoriteDbHelper favoriteDbHelper;
    private Movie favoriteMovie;
    public final AppCompatActivity currentActivity = DetailActivity.this;

    Movie movie;
    String thumbnail, movieName, synopsis, rating, dateOfRelease;
    int movieId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        thumbnail_image_header = findViewById(R.id.thumbnail_image_header);
        plotSynopsis = findViewById(R.id.plotSynopsis);
        userRating = findViewById(R.id.userRating);
        releaseDate = findViewById(R.id.releaseDate);

        Intent intent = getIntent();
        if (intent.hasExtra("movies")) {
            movie = getIntent().getParcelableExtra("movies");

            thumbnail = movie.getPosterPath();
            movieName = movie.getOriginalTitle();
            synopsis = movie.getOverview();
            rating = Double.toString(movie.getVoteAverage());
            dateOfRelease = movie.getReleaseDate();
            movieId = movie.getId();

            String poster = "https://image.tmdb.org/t/p/w500" + thumbnail;

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.load);

            Glide.with(this)
                    .setDefaultRequestOptions(requestOptions)
                    .load(poster)
                    .into(thumbnail_image_header);

            plotSynopsis.setText(synopsis);
            userRating.setText(rating);
            releaseDate.setText(dateOfRelease);

            ((CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar)).setTitle(movieName);
        } else {
            Toast.makeText(this, "No API Data", Toast.LENGTH_SHORT).show();
        }

        MaterialFavoriteButton favoriteButton = findViewById(R.id.favoriteButton);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        favoriteButton.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
            @Override
            public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                if (favorite) {
                    SharedPreferences.Editor editor = getSharedPreferences("com.android.nirmesh.tmdb.DetailActivity",
                            MODE_PRIVATE).edit();
                    editor.putBoolean("Favorite Added", true);
                    editor.commit();

                    saveFavorite();

                    Snackbar.make(buttonView, "Added to Favorite", Snackbar.LENGTH_SHORT).show();
                } else {
                    int movieID = getIntent().getExtras().getInt("id");
                    favoriteDbHelper = new FavoriteDbHelper(DetailActivity.this);
                    favoriteDbHelper.deleteFavorite(movieID);

                    SharedPreferences.Editor editor = getSharedPreferences("com.android.nirmesh.tmdb.DetailActivity",
                            MODE_PRIVATE).edit();
                    editor.putBoolean("Favorite Removed", true);
                    editor.commit();

                    Snackbar.make(buttonView, "Removed from Favorite", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        initViews();
    }

    private void initViews() {
        trailerList = new ArrayList<>();
        adapter = new TrailerAdapter(this, trailerList);

        recycler_view_trailer = findViewById(R.id.recycler_view_trailer);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recycler_view_trailer.setLayoutManager(mLayoutManager);
        recycler_view_trailer.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        loadJSON();
    }

    private void loadJSON() {

        try {
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please obtain your API Key from themoviedb.org",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Client Client = new Client();
            Service apiService = Client.getClient().create(Service.class);

            Call<TrailerResponse> call = apiService.getMovieTrailer(movieId, BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<TrailerResponse>() {
                @Override
                public void onResponse(Call<TrailerResponse> call, Response<TrailerResponse> response) {
                    List<Trailer> trailer = response.body().getResults();
                    recycler_view_trailer.setAdapter(new TrailerAdapter(getApplicationContext(), trailer));
                    recycler_view_trailer.smoothScrollToPosition(0);
                }

                @Override
                public void onFailure(Call<TrailerResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(DetailActivity.this, "Error fetching trailer data", Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void saveFavorite() {
        favoriteDbHelper = new FavoriteDbHelper(currentActivity);
        favoriteMovie = new Movie();

        Double rate = movie.getVoteAverage();

        favoriteMovie.setId(movieId);
        favoriteMovie.setOriginalTitle(movieName);
        favoriteMovie.setPosterPath(thumbnail);
        favoriteMovie.setVoteAverage(rate);
        favoriteMovie.setOverview(synopsis);

        favoriteDbHelper.addFavorite(favoriteMovie);
    }
}
