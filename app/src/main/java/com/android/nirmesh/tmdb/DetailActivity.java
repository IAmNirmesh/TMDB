package com.android.nirmesh.tmdb;

import android.content.Intent;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

public class DetailActivity extends AppCompatActivity {

    TextView title, plotSynopsis, userRating, releaseDate;
    ImageView thumbnail_image_header;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        initCollapsingToolbar();

        thumbnail_image_header = findViewById(R.id.thumbnail_image_header);
        title = findViewById(R.id.title);
        plotSynopsis = findViewById(R.id.plotSynopsis);
        userRating = findViewById(R.id.userRating);
        releaseDate = findViewById(R.id.releaseDate);

        Intent intent = getIntent();
        if (intent.hasExtra("original_title")) {
            String thumbnail = getIntent().getExtras().getString("poster_path");
            String movieName = getIntent().getExtras().getString("original_title");
            String synopsis = getIntent().getExtras().getString("overview");
            String rating = getIntent().getExtras().getString("vote_average");
            String dateOfRelease = getIntent().getExtras().getString("release_date");

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.load);

            Glide.with(this)
                    .setDefaultRequestOptions(requestOptions)
                    .load(thumbnail)
                    .into(thumbnail_image_header);

            title.setText(movieName);
            plotSynopsis.setText(synopsis);
            userRating.setText(rating);
            releaseDate.setText(dateOfRelease);
        } else {
            Toast.makeText(this, "No API Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void initCollapsingToolbar() {
        final CollapsingToolbarLayout collapsing_toolbar = findViewById(R.id.collapsing_toolbar);
        collapsing_toolbar.setTitle(" ");

        AppBarLayout appbar = findViewById(R.id.appbar);
        appbar.setExpanded(true);

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = false;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsing_toolbar.setTitle(getString(R.string.movie_details));
                    isShow = true;
                } else if (isShow) {
                    collapsing_toolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });
    }
}
