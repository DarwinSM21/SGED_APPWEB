package org.uteq.backend.common.ia;

import java.util.List;

public interface AIFeedbackGenerator {
    FeedbackResult generatePlayerComment(AnonymousPlayerProfile profile);
    FeedbackResult generateLineupComment(List<AnonymousPlayerProfile> lineup);
    boolean isAvailable();

    record FeedbackResult(String text, String reason) {
        public static FeedbackResult ok(String text) { return new FeedbackResult(text, null); }
        public static FeedbackResult unavailable(String reason) { return new FeedbackResult(null, reason); }
        public boolean isAvailable() { return text != null && !text.isBlank(); }
    }
}
