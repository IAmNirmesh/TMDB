package com.android.nirmesh.tmdb;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nirmesh.tmdb.adapter.ReviewAdapter;
import com.android.nirmesh.tmdb.adapter.TrailerAdapter;
import com.android.nirmesh.tmdb.api.Client;
import com.android.nirmesh.tmdb.api.Service;
import com.android.nirmesh.tmdb.data.FavoriteContract;
import com.android.nirmesh.tmdb.data.FavoriteDbHelper;
import com.android.nirmesh.tmdb.model.Movie;
import com.android.nirmesh.tmdb.model.Review;
import com.android.nirmesh.tmdb.model.ReviewResult;
import com.android.nirmesh.tmdb.model.Trailer;
import com.android.nirmesh.tmdb.model.TrailerResponse;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.ivbaranov.mfb.MaterialFavoriteButton;
import com.takusemba.multisnaprecyclerview.MultiSnapRecyclerView;

import java.io.File;
import java.io.FileOutputStream;
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

    private SQLiteDatabase mSQLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FavoriteDbHelper dbHelper = new FavoriteDbHelper(this);
        mSQLiteDatabase = dbHelper.getWritableDatabase();

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

        if (existsInFavorite(movieName)) {
            favoriteButton.setFavorite(true);
            favoriteButton.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
                @Override
                public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                    if (favorite == true) {
                        saveFavorite();
                        Snackbar.make(buttonView, "Added to Favorites", Snackbar.LENGTH_SHORT).show();
                    } else {
                        favoriteDbHelper = new FavoriteDbHelper(DetailActivity.this);
                        favoriteDbHelper.deleteFavorite(movieId);
                        Snackbar.make(buttonView, "Removed From Favorites", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            favoriteButton.setOnFavoriteChangeListener(new MaterialFavoriteButton.OnFavoriteChangeListener() {
                @Override
                public void onFavoriteChanged(MaterialFavoriteButton buttonView, boolean favorite) {
                    if (favorite == true) {
                        saveFavorite();
                        Snackbar.make(buttonView, "Added to Favorites", Snackbar.LENGTH_SHORT).show();
                    } else {
                        int movieID = getIntent().getExtras().getInt("id");
                        favoriteDbHelper = new FavoriteDbHelper(DetailActivity.this);
                        favoriteDbHelper.deleteFavorite(movieID);
                        Snackbar.make(buttonView, "Removed From Favorites", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }

        initViews();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.share:
                shareContent();
                return true;
        }

        return super.onOptionsItemSelected(item);
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

        loadReviews();
    }

    private void loadJSON() {
        try {
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please obtain your API Key",
                        Toast.LENGTH_SHORT).show();
                return;
            } else {

                Client Client = new Client();
                Service apiService = Client.getClient().create(Service.class);

                Call<TrailerResponse> call = apiService.getMovieTrailer(movieId, BuildConfig.THE_MOVIE_DB_API_TOKEN);
                call.enqueue(new Callback<TrailerResponse>() {
                    @Override
                    public void onResponse(Call<TrailerResponse> call, Response<TrailerResponse> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                List<Trailer> trailer = response.body().getResults();

                                MultiSnapRecyclerView recycler_view_trailer = findViewById(R.id.recycler_view_trailer);
                                LinearLayoutManager firstManager = new LinearLayoutManager(
                                        getApplicationContext(),
                                        LinearLayoutManager.VERTICAL,
                                        false);
                                recycler_view_trailer.setLayoutManager(firstManager);

                                recycler_view_trailer.setAdapter(new TrailerAdapter(getApplicationContext(), trailer));
                                recycler_view_trailer.smoothScrollToPosition(0);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<TrailerResponse> call, Throwable t) {
                        Log.d("Error", t.getMessage());
                        Toast.makeText(DetailActivity.this, "Error in Fetching Trailer", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadReviews() {
        try {
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Please get your API Key",
                        Toast.LENGTH_SHORT).show();
                return;
            } else {

                Client Client = new Client();
                Service apiService = Client.getClient().create(Service.class);

                Call<Review> call = apiService.getReview(movieId, BuildConfig.THE_MOVIE_DB_API_TOKEN);
                call.enqueue(new Callback<Review>() {
                    @Override
                    public void onResponse(Call<Review> call, Response<Review> response) {
                        if (response.isSuccessful()) {
                            if (response.body() != null) {
                                List<ReviewResult> reviewResults = response.body().getResults();

                                MultiSnapRecyclerView recyclerViewReview = findViewById(R.id.recyclerViewReview);
                                LinearLayoutManager firstManager = new LinearLayoutManager(
                                        getApplicationContext(),
                                        LinearLayoutManager.VERTICAL,
                                        false);
                                recyclerViewReview.setLayoutManager(firstManager);

                                recyclerViewReview.setAdapter(new ReviewAdapter(getApplicationContext(), reviewResults));
                                recyclerViewReview.smoothScrollToPosition(0);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Call<Review> call, Throwable t) {
                        Log.d("Error", t.getMessage());
                        Toast.makeText(DetailActivity.this, "Error in Fetching Reviews", Toast.LENGTH_SHORT).show();
                    }
                });
            }

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

    private boolean existsInFavorite(String searchItem) {
        String[] projection = {
                FavoriteContract.FavoriteEntry._ID,
                FavoriteContract.FavoriteEntry.COLUMN_MOVIEID,
                FavoriteContract.FavoriteEntry.COLUMN_TITLE,
                FavoriteContract.FavoriteEntry.COLUMN_USERRATING,
                FavoriteContract.FavoriteEntry.COLUMN_POSTER_PATH,
                FavoriteContract.FavoriteEntry.COLUMN_PLOT_SYNOPSIS
        };

        String selection = FavoriteContract.FavoriteEntry.COLUMN_TITLE + "=?";
        String[] selectionArgs = { searchItem };
        String limit = "1";

        Cursor cursor = mSQLiteDatabase.query(
                FavoriteContract.FavoriteEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null,
                limit);

        boolean exists = (cursor.getCount() > 0);
        cursor.close();

        return exists;
    }

    private void shareContent() {
        Bitmap bitmap = getBitmapFromView(thumbnail_image_header);
        try {
            File file = new File(this.getExternalCacheDir(),"logicchip.png");
            FileOutputStream fOut = new FileOutputStream(file);

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();

            file.setReadable(true, false);

            final Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(Intent.EXTRA_TEXT, movieName);
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            intent.setType("image/png");

            startActivity(Intent.createChooser(intent, "Share image via"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getBitmapFromView(View thumbnail_image_header) {
        Bitmap returnedBitmap = Bitmap.createBitmap(thumbnail_image_header.getWidth(), thumbnail_image_header.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(returnedBitmap);

        Drawable bgDrawable =thumbnail_image_header.getBackground();

        if (bgDrawable!=null) {
            bgDrawable.draw(canvas);
        }   else{
            canvas.drawColor(Color.WHITE);
        }

        thumbnail_image_header.draw(canvas);

        return returnedBitmap;
    }
}
