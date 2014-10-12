package jp.gr.java_conf.daisy.n2mu;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.google.code.linkedinapi.client.LinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.ProfileType;
import com.google.code.linkedinapi.schema.Education;
import com.google.code.linkedinapi.schema.Educations;
import com.google.code.linkedinapi.schema.Person;
import com.google.code.linkedinapi.schema.Position;
import com.google.code.linkedinapi.schema.Positions;
import com.google.code.linkedinapi.schema.ThreePastPositions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KeywordHelper {
    private final Context mContext;
    private final LinkedInApiClientFactory mLinkedInClientFactory
            = LinkedInApiClientFactory.newInstance(BuildConfig.LINKEDIN_CONSUMER_KEY, BuildConfig.LINKEDIN_CONSUMER_SECRET);
    private final LinkedInApiClient mLinkedInClient;

    public KeywordHelper(Context context) {
        mContext = context;
        String token = Preferences.getDefault(context).getString(Preferences.KEY_LINKEDIN_ACCESS_TOKEN, "");
        String tokenSecret = Preferences.getDefault(context).getString(Preferences.KEY_LINKEDIN_ACCESS_TOKEN_SECRET, "");
        if (token.length() == 0 || tokenSecret.length() == 0) {
            throw new IllegalStateException("Token or token secret does not exist");
        }
        mLinkedInClient = mLinkedInClientFactory.createLinkedInApiClient(token, tokenSecret);
    }

    public void fetchKeywordWithLinkedInId(final String linkedInId, final OnKeywordObtainedListener onKeywordObtainedListener) {
        final ProgressDialog dialog = new ProgressDialog(mContext);
        dialog.setIndeterminate(true);
        dialog.setMessage("Fetching keywords from linkedin...");
        dialog.show();
        new AsyncTask<Void, Void, Set<String>>() {
            @Override
            protected Set<String> doInBackground(Void... params) {

                Person profile = mLinkedInClient.getProfileById(linkedInId);
                Set<String> keywords = new HashSet<String>();
                addIfNotNull(keywords, profile.getInterests());
                Educations educations = profile.getEducations();
                if (educations != null && educations.getEducationList() != null) {
                    for (Education education: educations.getEducationList()) {
                        String schoolName = education.getSchoolName();
                        if (schoolName != null) {
                            // Reduce # of chars
                            schoolName = schoolName.replace("University", "Univ.");
                        }
                        addIfNotNull(keywords, schoolName);
                        addIfNotNull(keywords, education.getFieldOfStudy());
                    }
                }
                ThreePastPositions pastPositions = profile.getThreePastPositions();
                if (pastPositions != null && pastPositions.getPositionList() != null) {
                    for (Position position: pastPositions.getPositionList()) {
                        addIfNotNull(keywords, position.getCompany() != null ? null : position.getCompany().getName());
                        addIfNotNull(keywords, position.getTitle());
                    }
                }
                if (profile.getPatents() != null && profile.getPatents().getTotal() > 0) {
                    keywords.add("Patent holder");
                }
                return keywords;
            }

            @Override
            protected void onPostExecute(Set<String> strings) {
                super.onPostExecute(strings);
                dialog.dismiss();
                onKeywordObtainedListener.keywordObtained(strings);
            }
        }.execute();
    }

    private <E> void addIfNotNull(Collection<E> lists, E element) {
        if (element != null) {
            lists.add(element);
        }
    }

    public interface OnKeywordObtainedListener {
        public void keywordObtained(Set<String> keywords);
    }
}
