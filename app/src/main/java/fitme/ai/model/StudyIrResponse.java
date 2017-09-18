package fitme.ai.model;

/**
 * Created by hongy on 2017/7/25.
 */

public interface StudyIrResponse {
    void onStudySuccess(String irCode);
    void onStudyFailed();
}
