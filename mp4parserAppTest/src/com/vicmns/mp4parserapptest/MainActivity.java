/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vicmns.mp4parserapptest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.TimeToSampleBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.vicmns.mp4parserapptest.R;

public class MainActivity extends Activity implements OnClickListener {
	public static final String TAG = "MP4PARSER";
	private Button mergeBttn;
	private Button trimmBttn;
	private Context context;
	private ArrayList<String> videosToMerge;
	private AsyncTask<String, Integer, String> mergeVideos;
	private AsyncTask<Void, Void, Void> trimmVideo;
	private String workingPath;
	
	static final long[] ROTATE_0 = new long[]{1, 0, 0, 1, 0, 0, 1, 0, 0};
    static final long[] ROTATE_90 = new long[]{0, 1, -1, 0, 0, 0, 1, 0, 0};
    static final long[] ROTATE_180 = new long[]{-1, 0, 0, -1, 0, 0, 1, 0, 0};
    static final long[] ROTATE_270 = new long[]{0, -1, 1, 0, 0, 0, 1, 0, 0};
    
    private long[] rotate0 = new long[] {0x00010000,0,0,0,0x00010000,0,0,0,0x40000000};
    private long[] rotate90 = new long[] {0,0x00010000,0,-0x00010000,0,0,0,0,0x40000000};
    private long[] rotate180 = new long[] {0x00010000,0,0,0,0x00010000,0,0,0, 0x40000000};
    private long[] rotate270 = new long[] {-0x00010000,0,0,0,-0x00010000,0,0,0, 0x40000000};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.context = this;
		
		videosToMerge = new ArrayList<String>();
		videosToMerge.add("vid_demo_1.mp4");
		videosToMerge.add("vid_demo_2.mp4");
		videosToMerge.add("vid_demo_3.mp4");
		
		//Set the working path
		this.workingPath = Environment.getExternalStorageDirectory() + "/com.vicmns.mp4parserapptest";
		
		initializeObjects();
		setOnClickListeners();
	}

	@Override
	protected void onStop() {
		if(mergeVideos != null) {
			mergeVideos.cancel(true);
		}
		if(trimmVideo != null)
			trimmVideo.cancel(true);
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private void initializeObjects() {
		mergeBttn = (Button) findViewById(R.id.mergeBttn);
		trimmBttn = (Button) findViewById(R.id.trimmBttn);
	}
	
	private void setOnClickListeners() {
		mergeBttn.setOnClickListener(this);
		trimmBttn.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		
		switch(view.getId()){
			case R.id.mergeBttn:
				mergeVideos = new MergeVideos(workingPath, videosToMerge).execute();
			break;
			case R.id.trimmBttn:
				trimmVideo = new TrimmVideo(workingPath + "/big_buck_bunn_short.mp4", 3, 6).execute();
			break;
		}
		
	}
	
	private class MergeVideos extends AsyncTask<String, Integer, String> {
		
		//The working path where the video files are located
		private String workingPath; 
		//The file names to merge
		private ArrayList<String> videosToMerge;
		//Dialog to show to the user
		private ProgressDialog progressDialog;
		
		private MergeVideos(String workingPath, ArrayList<String> videosToMerge) {
			this.workingPath = workingPath;
			this.videosToMerge = videosToMerge;
		}
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(context,
					"Merging videos", "Please wait...", true);
		};
		
		@Override
		protected String doInBackground(String... params) {
			int count = videosToMerge.size();
			try {
				Movie[] inMovies = new Movie[count];
				for (int i = 0; i < count; i++) {
					File file = new File(workingPath, videosToMerge.get(i));
					if(file.exists()) {
						FileInputStream fis = new FileInputStream(file);
						FileChannel fc = fis.getChannel();
						inMovies[i] = MovieCreator.build(fc);
						fis.close();
						fc.close();
					}
				}
				List<Track> videoTracks = new LinkedList<Track>();
				List<Track> audioTracks = new LinkedList<Track>();
				
				for (Movie m : inMovies) {
					for (Track t : m.getTracks()) {
						if (t.getHandler().equals("soun")) {
							audioTracks.add(t);
						}
						if (t.getHandler().equals("vide")) {
							videoTracks.add(t);
						}
						if (t.getHandler().equals("")) {
							
						}
					}
				}
				
				Movie result = new Movie();
				
				if (audioTracks.size() > 0) {
					result.addTrack(new AppendTrack(audioTracks
							.toArray(new Track[audioTracks.size()])));
				}
				if (videoTracks.size() > 0) {
					result.addTrack(new AppendTrack(videoTracks
							.toArray(new Track[videoTracks.size()])));
				}
				IsoFile out = new DefaultMp4Builder()
				.build(result);
				
				//rotate video
				
				out.getMovieBox().getMovieHeaderBox().setMatrix(ROTATE_270);
				
				long timestamp=new Date().getTime();
				String timestampS="" + timestamp;
				
				File storagePath = new File(workingPath);             
				storagePath.mkdirs();  
				
				File myMovie = new File(storagePath, String.format("output-%s.mp4", timestampS)); 
				
				FileOutputStream fos = new FileOutputStream(myMovie);
				FileChannel fco = fos.getChannel();
				fco.position(0);
				out.getBox(fco);
				fco.close();
				fos.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String mFileName = Environment.getExternalStorageDirectory()
					.getAbsolutePath();
			mFileName += "/output.mp4";
			return mFileName;
		}
		
		@Override
		protected void onPostExecute(String value) {
			super.onPostExecute(value);
			progressDialog.dismiss();
		}
		
	}
	
	private class TrimmVideo extends AsyncTask<Void, Void, Void> {
		private String mediaPath;
		private double startTime;
		private double endTime;
		private int length;
		private ProgressDialog progressDialog;
		
		private TrimmVideo(String mediaPath, int startTime, int length) {
			this.mediaPath = mediaPath;
			this.startTime = startTime;
			this.length = length;
			this.endTime = this.startTime + this.length;
		}
		
		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(context,
					"Trimming videos", "Please wait...", true);
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			trimVideo();
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			progressDialog.dismiss();
			super.onPostExecute(result);
		}

		private void trimVideo() {
			try {  
				File file = new File(mediaPath);
				FileInputStream fis = new FileInputStream(file);
				FileChannel in = fis.getChannel();
				Movie movie = MovieCreator.build(in);
				
				List<Track> tracks = movie.getTracks();
				movie.setTracks(new LinkedList<Track>());   
				
				boolean timeCorrected = false;

		        // Here we try to find a track that has sync samples. Since we can only start decoding
		        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
		        // such a frame
		        for (Track track : tracks) {
		            if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
		                if (timeCorrected) {
		                    // This exception here could be a false positive in case we have multiple tracks
		                    // with sync samples at exactly the same positions. E.g. a single movie containing
		                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

		                    //throw new RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.");
		                } else {
		                	startTime = correctTimeToNextSyncSample(track, startTime);
		                	timeCorrected = true;		                	
		                }
		            }
		        }

		        for (Track track : tracks) {
		            long currentSample = 0;
		            double currentTime = 0;
		            long startSample = -1;
		            long endSample = -1;

		            for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
		                TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
		                for (int j = 0; j < entry.getCount(); j++) {
		                    // entry.getDelta() is the amount of time the current sample covers.

		                    if (currentTime <= startTime) {
		                        // current sample is still before the new starttime
		                        startSample = currentSample;
		                    } else if (currentTime <= endTime) {
		                        // current sample is after the new start time and still before the new endtime
		                        endSample = currentSample;
		                    } else {
		                        // current sample is after the end of the cropped video
		                        break;
		                    }
		                    currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
		                    currentSample++;
		                }
		            }
		            movie.addTrack(new CroppedTrack(track, startSample, endSample));
		        }
				//if(startTime==length)
					//throw new Exception("times are equal, something went bad in the conversion");
				
				IsoFile out = new DefaultMp4Builder().build(movie);        
				
				File storagePath = new File(workingPath);             
				storagePath.mkdirs();       
				
				long timestamp=new Date().getTime();
				String timestampS="" + timestamp;
				
				File myMovie = new File(storagePath, String.format("output-%s-%f-%d.mp4", timestampS, startTime*1000, length*1000)); 
				
				FileOutputStream fos = new FileOutputStream(myMovie);
				FileChannel fc = fos.getChannel();
				out.getBox(fc);
				
				fc.close();
				fos.close();
				fis.close();
				in.close();
			} catch(Exception e)
			{
				e.printStackTrace();
			}
		}
		
		private double correctTimeToNextSyncSample(Track track, double cutHere) {
	        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
	        long currentSample = 0;
	        double currentTime = 0;
	        for (int i = 0; i < track.getDecodingTimeEntries().size(); i++) {
	            TimeToSampleBox.Entry entry = track.getDecodingTimeEntries().get(i);
	            for (int j = 0; j < entry.getCount(); j++) {
	                if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
	                    // samples always start with 1 but we start with zero therefore +1
	                    timeOfSyncSamples[Arrays.binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
	                }
	                currentTime += (double) entry.getDelta() / (double) track.getTrackMetaData().getTimescale();
	                currentSample++;
	            }
	        }
	        for (double timeOfSyncSample : timeOfSyncSamples) {
	            if (timeOfSyncSample > cutHere) {
	                return timeOfSyncSample;
	            }
	        }
	        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
	    }
	}
}
