package com.tc2r.sgquizapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tc2r.sgquizapp.fragments.QuestionFragment;
import com.tc2r.sgquizapp.models.Answer;
import com.tc2r.sgquizapp.models.Question;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {
	// Static Variables
	private final static int QUIZ_SIZE = 10;

	// UI Variables
	private LinearLayout fragContainer;
	private TextView titleTv, scoreTv;

	// Fragments and Model Variables
	private QuestionFragment newFragment;
	private ArrayList<Question> quizList;
	private ArrayList<Question> testList;
	private ArrayList<Answer> answersList;
	private boolean selectedQuestion[];

	// Variables
	private Random random;
	private int currentQuestion = 0;
	private int numOfCorrect = 0;
	private int scorePer= 0;
	private double pointPerQ, score;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initalize and assignments
		titleTv = (TextView) findViewById(R.id.title_tv);
		scoreTv = (TextView) findViewById(R.id.score_tv);
		testList = new ArrayList<>();
		quizList = new ArrayList<>();
		answersList = new ArrayList<>();
		random = new Random();

		// get Wrong answers from server
		getAnswers();

	}

	public void getAnswers() {

		// Creates an AsyncTask to make a call to my server php file to return
		// a json String array of incorrect answers.

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... strings) {
				// Create client and request, this time using OkHttp.
				OkHttpClient client = new OkHttpClient();
				Request request = new Request.Builder()
								.url("https://tchost.000webhostapp.com/getquiz_ans_sg.php?")
								.build();

				try {
					// Check for a response, if recieved parse through it and assign rows to Answer Objects.
					Response response = client.newCall(request).execute();
					JSONArray array = new JSONArray(response.body().string());

					for (int i = 0; i < array.length(); i++) {
						JSONObject object = array.getJSONObject(i);
						String tempAnswer = object.getString("topic");

						Answer temp;

						if (object.getString("details") != null) {
							String tempDetails = object.getString("details");
							temp = new Answer(tempAnswer, tempDetails);
						} else {
							temp = new Answer(tempAnswer);
						}
						answersList.add(temp);
					}

				} catch (JSONException | IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);
				// Once complete
				// Get Questions From Database Online
				getQuestions();


			}
		};
		// execute the AsyncTask created above
		task.execute();

	}

	public void getQuestions() {
		// Same as getAnswers

		AsyncTask<String, Void, Void> task = new AsyncTask<String, Void, Void>() {

			@Override
			protected Void doInBackground(String... strings) {
				OkHttpClient client = new OkHttpClient();
				Request request = new Request.Builder()
								.url("https://tchost.000webhostapp.com/getquiz_sg.php?")
								.build();

				try {
					Response response = client.newCall(request).execute();
					JSONArray array = new JSONArray(response.body().string());

					for (int i = 0; i < array.length(); i++) {
						JSONObject object = array.getJSONObject(i);
						String tempQuestion = object.getString("question");
						String tempKey = object.getString("key");
						Question temp;

						if (object.getString("summary") != null) {
							String tempSummary = object.getString("summary");
							temp = new Question(tempQuestion, tempKey, tempSummary);
						} else {
							temp = new Question(tempQuestion, tempKey);
						}
						quizList.add(temp);
					}

				} catch (JSONException | IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				super.onPostExecute(aVoid);

				// once questions and answers have been retrieved, create a quiz.
				createQuiz();
			}
		};
		task.execute();

	}

	private void createQuiz() {

		// set booleanArray to be same size as quizList

		selectedQuestion = new boolean[quizList.size()];

		// set an int to a random number in the quizList
		int randNum = random.nextInt(quizList.size());
		int i = 0;

		// sets the score system for quiz.
		// TODO: 5/31/2017 maybe remove this and simply divide correct answers by total questions
		pointPerQ = 1.0 / QUIZ_SIZE;


		while (i < QUIZ_SIZE) {
			//Log.wtf("testList Size: ", String.valueOf(testList.size()) + " Vs "+ String.valueOf(quizSize) );
			// if boolean at randNum in selectedQuestion is false
			if (!selectedQuestion[randNum]) {
				// Add position randNum to test list;
				testList.add(quizList.get(randNum));
				// set this question selected to true.
				selectedQuestion[randNum] = true;
				i++;
			} else {
				// if question already selected, change randNum;
				randNum = random.nextInt(quizList.size());
			}
		}
		// On first run, start quiz without updating score
		nextQuestion(false);
	}

	public void nextQuestion(boolean correctAnswer) {

		// if previous question was answered correctly
		// update variables accordingly.
		if (correctAnswer) {
			numOfCorrect++;
			score += pointPerQ;
			scorePer = (int) (score * 100);
			scoreTv.setText(getString(R.string.score_display_text) + String.valueOf(scorePer));
		}
		// if quiz is not complete, continue quiz with new QuestionFragment
		if (currentQuestion < QUIZ_SIZE) {
			newFragment = QuestionFragment.newInstance(testList.get(currentQuestion), answersList);
			currentQuestion++;
			titleTv.setText(getString(R.string.question_display_text) + Integer.toString(currentQuestion) + " of " + Integer.toString(QUIZ_SIZE));
			fragContainer = (LinearLayout) findViewById(R.id.fragment_container);
			FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
			ft.replace(fragContainer.getId(), newFragment);
			ft.commit();
		}else{
			// Quiz is over, go to final page!
			// create intent
			Intent intent = new Intent(this, ScoreActivity.class);

			// add variables to send.
			intent.putExtra("scorePercentage", scorePer);
			intent.putExtra("quizSize", QUIZ_SIZE);
			intent.putExtra("numCorrect", numOfCorrect);

			// use intent.
			startActivity(intent);
		}
	}
	@Override
	public void fragmentInitialized() {

	}
}
