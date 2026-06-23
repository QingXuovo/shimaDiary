package com.shima.diary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.common.Result;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.User;
import com.shima.diary.service.DiaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI心情分析控制器
 * 基于日记内容的关键词情绪分析
 */
@RestController
@RequestMapping("/diary/ai")
public class MoodAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(MoodAnalysisController.class);

    @Autowired
    private DiaryService diaryService;

    // 正面情绪关键词
    private static final List<String> POSITIVE_WORDS = Arrays.asList(
        "开心", "快乐", "高兴", "幸福", "满足", "兴奋", "激动", "喜悦", "愉快", "欣慰",
        "感动", "温暖", "美好", "顺利", "成功", "进步", "收获", "成长", "喜欢", "爱",
        "希望", "期待", "感恩", "感谢", "棒", "优秀", "出色", "精彩", "完美", "赞",
        "happy", "good", "great", "love", "wonderful", "excellent", "amazing", "joy",
        "开心死了", "太棒了", "非常好", "超级开心", "心情大好"
    );

    // 负面情绪关键词
    private static final List<String> NEGATIVE_WORDS = Arrays.asList(
        "难过", "伤心", "悲伤", "痛苦", "沮丧", "失落", "失望", "焦虑", "担心", "害怕",
        "恐惧", "愤怒", "生气", "烦躁", "郁闷", "压抑", "孤独", "寂寞", "疲惫", "累",
        "失败", "挫折", "困难", "问题", "麻烦", "糟糕", "讨厌", "恨", "哭", "泪",
        "sad", "bad", "terrible", "hate", "angry", "worried", "stress", "upset",
        "太难了", "受不了", "烦死了", "心情不好", "不开心"
    );

    // 平静/中性情绪关键词
    private static final List<String> CALM_WORDS = Arrays.asList(
        "平静", "安静", "宁静", "放松", "舒适", "悠闲", "自在", "淡定", "从容", "平和",
        "日常", "普通", "正常", "一般", "还行", "还可以", "不错", "挺好",
        "calm", "peaceful", "relax", "quiet", "normal", "okay", "fine"
    );

    private Long getUserId(HttpSession session) {
        User user = (User) session.getAttribute("user");
        Long userId = user != null ? user.getId() : 1L;
        return userId;
    }

    /**
     * 分析单篇日记的情绪
     */
    @GetMapping("/analyze/{id}")
    public Result<Map<String, Object>> analyzeDiary(@PathVariable Long id, HttpSession session) {
        logger.info("分析日记情绪: id={}", id);
        Long userId = getUserId(session);
        try {
            Diary diary = diaryService.getById(id);
            if (diary == null) {
                return Result.error("日记不存在");
            }
            if (!diary.getUserId().equals(userId)) {
                return Result.error("无权操作");
            }

            Map<String, Object> analysis = analyzeText(diary.getContent(), diary.getTitle());
            analysis.put("diaryId", diary.getId());
            analysis.put("diaryTitle", diary.getTitle());
            analysis.put("diaryDate", diary.getDiaryDate() != null ? diary.getDiaryDate().toString() : "");

            logger.info("情绪分析完成: id={}, sentiment={}", id, analysis.get("sentiment"));
            return Result.success(analysis);
        } catch (Exception e) {
            logger.error("情绪分析失败: id={}", id, e);
            return Result.error("分析失败");
        }
    }

    /**
     * 获取用户近期情绪趋势
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getMoodTrend(
            @RequestParam(defaultValue = "30") int days,
            HttpSession session) {
        logger.info("获取情绪趋势: days={}", days);
        Long userId = getUserId(session);
        try {
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(days - 1);

            List<Diary> diaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .ge(Diary::getDiaryDate, startDate)
                .le(Diary::getDiaryDate, endDate)
                .orderByAsc(Diary::getDiaryDate));

            List<Map<String, Object>> trendData = new ArrayList<>();
            int positiveCount = 0;
            int negativeCount = 0;
            int calmCount = 0;

            for (Diary diary : diaries) {
                Map<String, Object> analysis = analyzeText(diary.getContent(), diary.getTitle());
                Map<String, Object> item = new HashMap<>();
                item.put("date", diary.getDiaryDate() != null ? diary.getDiaryDate().toString() : "");
                item.put("title", diary.getTitle());
                item.put("sentiment", analysis.get("sentiment"));
                item.put("score", analysis.get("score"));
                item.put("positiveScore", analysis.get("positiveScore"));
                item.put("negativeScore", analysis.get("negativeScore"));
                trendData.add(item);

                String sentiment = (String) analysis.get("sentiment");
                if ("positive".equals(sentiment)) positiveCount++;
                else if ("negative".equals(sentiment)) negativeCount++;
                else calmCount++;
            }

            Map<String, Object> result = new HashMap<>();
            result.put("trend", trendData);
            result.put("summary", Map.of(
                "total", diaries.size(),
                "positive", positiveCount,
                "negative", negativeCount,
                "calm", calmCount,
                "positiveRate", diaries.isEmpty() ? 0 : (double) positiveCount / diaries.size(),
                "negativeRate", diaries.isEmpty() ? 0 : (double) negativeCount / diaries.size(),
                "calmRate", diaries.isEmpty() ? 0 : (double) calmCount / diaries.size()
            ));

            logger.info("情绪趋势分析完成: 共{}篇日记", diaries.size());
            return Result.success(result);
        } catch (Exception e) {
            logger.error("情绪趋势分析失败", e);
            return Result.error("分析失败");
        }
    }

    /**
     * 获取AI情绪建议
     */
    @GetMapping("/suggestion")
    public Result<Map<String, Object>> getSuggestion(HttpSession session) {
        logger.info("获取AI情绪建议");
        Long userId = getUserId(session);
        try {
            // 获取最近7天的日记
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = endDate.minusDays(6);

            List<Diary> recentDiaries = diaryService.list(new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .ge(Diary::getDiaryDate, startDate)
                .le(Diary::getDiaryDate, endDate)
                .orderByDesc(Diary::getDiaryDate));

            Map<String, Object> result = new HashMap<>();
            result.put("diaryCount", recentDiaries.size());

            if (recentDiaries.isEmpty()) {
                result.put("mood", "neutral");
                result.put("suggestion", "还没有最近的日记记录，试着写下今天的心情吧！记录生活是了解自己的第一步。");
                result.put("tips", Arrays.asList(
                    "每天花5分钟记录一件让你开心的事",
                    "尝试用文字表达你的感受，有助于情绪管理",
                    "设定一个固定的写日记时间，养成习惯"
                ));
            } else {
                // 分析最近日记的整体情绪
                int posScore = 0, negScore = 0;
                for (Diary diary : recentDiaries) {
                    Map<String, Object> analysis = analyzeText(diary.getContent(), diary.getTitle());
                    posScore += (int) analysis.get("positiveScore");
                    negScore += (int) analysis.get("negativeScore");
                }

                if (posScore > negScore * 1.5) {
                    result.put("mood", "positive");
                    result.put("suggestion", "最近你的状态很不错！保持积极的心态，继续记录生活中的美好瞬间。");
                    result.put("tips", Arrays.asList(
                        "继续保持写日记的好习惯",
                        "把你的快乐分享给朋友",
                        "回顾这些美好记录，在低落时给自己力量"
                    ));
                } else if (negScore > posScore * 1.5) {
                    result.put("mood", "negative");
                    result.put("suggestion", "最近似乎有些压力，记得照顾好自己的情绪。适当的休息和放松很重要。");
                    result.put("tips", Arrays.asList(
                        "尝试做一些让自己放松的事情，比如散步、听音乐",
                        "和朋友聊聊，分享你的感受",
                        "给自己一些独处的时间，不要给自己太大压力",
                        "如果持续感到困扰，可以考虑寻求专业帮助"
                    ));
                } else {
                    result.put("mood", "balanced");
                    result.put("suggestion", "最近的情绪比较平稳，这是很好的状态。继续保持生活的平衡。");
                    result.put("tips", Arrays.asList(
                        "尝试新的挑战，让生活更有活力",
                        "培养一个新的兴趣爱好",
                        "定期回顾自己的成长轨迹"
                    ));
                }
            }

            return Result.success(result);
        } catch (Exception e) {
            logger.error("获取AI建议失败", e);
            return Result.error("获取失败");
        }
    }

    /**
     * 分析文本情绪
     */
    private Map<String, Object> analyzeText(String content, String title) {
        Map<String, Object> result = new HashMap<>();

        String text = "";
        if (title != null) text += title + " ";
        if (content != null) text += content;
        text = text.toLowerCase();

        int positiveScore = 0;
        int negativeScore = 0;
        int calmScore = 0;

        for (String word : POSITIVE_WORDS) {
            if (text.contains(word.toLowerCase())) {
                positiveScore++;
            }
        }
        for (String word : NEGATIVE_WORDS) {
            if (text.contains(word.toLowerCase())) {
                negativeScore++;
            }
        }
        for (String word : CALM_WORDS) {
            if (text.contains(word.toLowerCase())) {
                calmScore++;
            }
        }

        int totalScore = positiveScore + negativeScore + calmScore;
        double score = 0;
        String sentiment;

        if (totalScore == 0) {
            sentiment = "neutral";
            score = 50;
        } else {
            score = (double) (positiveScore - negativeScore + 50) / (totalScore + 1) * 50 + 25;
            score = Math.max(0, Math.min(100, score));

            if (positiveScore > negativeScore) {
                sentiment = "positive";
            } else if (negativeScore > positiveScore) {
                sentiment = "negative";
            } else {
                sentiment = "neutral";
            }
        }

        result.put("sentiment", sentiment);
        result.put("score", Math.round(score * 100.0) / 100.0);
        result.put("positiveScore", positiveScore);
        result.put("negativeScore", negativeScore);
        result.put("calmScore", calmScore);

        // 情绪标签
        List<String> tags = new ArrayList<>();
        if (positiveScore > 0) tags.add("积极");
        if (negativeScore > 0) tags.add("消极");
        if (calmScore > 0) tags.add("平静");
        if (totalScore == 0) tags.add("中性");
        result.put("tags", tags);

        return result;
    }
}