package com.shima.diary.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.shima.diary.config.AIConfig;
import com.shima.diary.entity.ChatMessage;
import com.shima.diary.entity.ChatSession;
import com.shima.diary.entity.Diary;
import com.shima.diary.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI情感陪伴服务
 */
@Service
public class AICompanionService {
    
    private static final Logger logger = LoggerFactory.getLogger(AICompanionService.class);
    
    @Autowired
    private AIConfig aiConfig;
    
    @Autowired
    private ChatMessageService chatMessageService;
    
    @Autowired
    private ChatSessionService chatSessionService;
    
    @Autowired
    private DiaryService diaryService;
    
    @Autowired
    private UserService userService;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 发送消息并获取AI回复
     */
    public Map<String, Object> sendMessage(Long userId, String sessionId, String message, String messageType) {
        logger.info("AI对话请求: userId={}, sessionId={}, message={}", userId, sessionId, message);
        
        try {
            // 获取或创建会话
            ChatSession session = null;
            if (sessionId != null && !sessionId.isEmpty()) {
                session = chatSessionService.getSession(userId, sessionId);
            }
            if (session == null) {
                session = chatSessionService.createSession(userId, "companion", "AI陪伴对话");
            }
            
            // 保存用户消息
            ChatMessage userMessage = new ChatMessage();
            userMessage.setUserId(userId);
            userMessage.setSessionId(session.getSessionId());
            userMessage.setRole("user");
            userMessage.setContent(message);
            userMessage.setMessageType(messageType);
            userMessage.setCreateTime(LocalDateTime.now());
            
            // 分析情感
            Map<String, Object> sentiment = analyzeSentiment(message);
            userMessage.setSentiment((String) sentiment.get("sentiment"));
            userMessage.setSentimentScore((Double) sentiment.get("score"));
            chatMessageService.saveMessage(userMessage);
            
            // 获取对话历史
            List<ChatMessage> history = chatMessageService.getSessionMessages(userId, session.getSessionId());
            
            // 获取用户信息
            User user = userService.getById(userId);
            String userName = user != null ? user.getUsername() : "用户";
            
            // 获取用户最近的日记
            List<Diary> recentDiaries = getRecentDiaries(userId, 5);
            
            // 生成AI回复
            String aiResponse = generateAIResponse(userId, userName, message, history, recentDiaries, messageType);
            
            // 保存AI回复
            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setUserId(userId);
            assistantMessage.setSessionId(session.getSessionId());
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(aiResponse);
            assistantMessage.setMessageType("text");
            assistantMessage.setCreateTime(LocalDateTime.now());
            chatMessageService.saveMessage(assistantMessage);
            
            // 更新会话
            session.setMessageCount(session.getMessageCount() + 2);
            session.setLastMessageTime(LocalDateTime.now());
            if (session.getTitle() == null || session.getTitle().equals("AI陪伴对话")) {
                session.setTitle(generateSessionTitle(message));
            }
            chatSessionService.updateSession(session);
            
            // 返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("sessionId", session.getSessionId());
            result.put("message", aiResponse);
            result.put("sentiment", sentiment);
            result.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            logger.info("AI对话成功: response={}", aiResponse);
            return result;
            
        } catch (Exception e) {
            logger.error("AI对话失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "对话失败，请稍后重试");
            errorResult.put("message", "抱歉，我现在有点累了，请稍后再和我聊天吧~");
            return errorResult;
        }
    }
    
    /**
     * 生成AI回复
     */
    private String generateAIResponse(Long userId, String userName, String message, 
                                     List<ChatMessage> history, List<Diary> recentDiaries, String messageType) {
        
        // 如果AI未启用或使用模拟模式
        if (!aiConfig.isEnabled() || "mock".equals(aiConfig.getProvider())) {
            return generateMockResponse(userId, userName, message, recentDiaries, messageType);
        }
        
        try {
            // 根据提供商调用不同的AI
            String provider = aiConfig.getProvider();
            switch (provider.toLowerCase()) {
                case "openai":
                    return callOpenAI(message, history, userName, recentDiaries);
                case "wenxin":
                    return callWenxin(message, history, userName, recentDiaries);
                case "qianwen":
                    return callQianwen(message, history, userName, recentDiaries);
                default:
                    return generateMockResponse(userId, userName, message, recentDiaries, messageType);
            }
        } catch (Exception e) {
            logger.error("调用AI失败，使用模拟响应", e);
            return generateMockResponse(userId, userName, message, recentDiaries, messageType);
        }
    }
    
    /**
     * 调用OpenAI API
     */
    private String callOpenAI(String message, List<ChatMessage> history, String userName, List<Diary> recentDiaries) {
        try {
            AIConfig.OpenAIConfig config = aiConfig.getOpenai();
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            String systemPrompt = buildSystemPrompt(userName, recentDiaries);
            messages.add(Map.of("role", "system", "content", systemPrompt));
            
            // 添加历史消息（最近10条）
            int startIndex = Math.max(0, history.size() - 10);
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
            
            // 添加当前消息
            messages.add(Map.of("role", "user", "content", message));
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("messages", messages);
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());
            
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(requestBody), headers);
            
            // 发送请求
            String url = config.getBaseUrl() + "/chat/completions";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.getBody());
            JSONArray choices = responseJson.getJSONArray("choices");
            if (choices != null && !choices.isEmpty()) {
                return choices.getJSONObject(0).getJSONObject("message").getString("content");
            }
            
        } catch (Exception e) {
            logger.error("调用OpenAI API失败", e);
        }
        
        return "抱歉，我现在无法回应，请稍后再试~";
    }
    
    /**
     * 调用文心一言API
     */
    private String callWenxin(String message, List<ChatMessage> history, String userName, List<Diary> recentDiaries) {
        // 文心一言API调用实现
        // 由于文心一言需要获取access_token，这里简化处理
        // 实际使用时需要先调用文心一言的token接口获取access_token
        logger.info("调用文心一言API");
        return generateMockResponse(null, userName, message, recentDiaries, "text");
    }
    
    /**
     * 调用通义千问API
     */
    private String callQianwen(String message, List<ChatMessage> history, String userName, List<Diary> recentDiaries) {
        try {
            AIConfig.QianwenConfig config = aiConfig.getQianwen();
            
            // 构建消息列表
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 系统提示
            String systemPrompt = buildSystemPrompt(userName, recentDiaries);
            messages.add(Map.of("role", "system", "content", systemPrompt));
            
            // 添加历史消息
            int startIndex = Math.max(0, history.size() - 10);
            for (int i = startIndex; i < history.size(); i++) {
                ChatMessage msg = history.get(i);
                messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
            }
            
            // 添加当前消息
            messages.add(Map.of("role", "user", "content", message));
            
            // 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", config.getModel());
            requestBody.put("input", Map.of("messages", messages));
            requestBody.put("parameters", Map.of(
                "max_tokens", config.getMaxTokens(),
                "temperature", config.getTemperature()
            ));
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());
            
            HttpEntity<String> entity = new HttpEntity<>(JSON.toJSONString(requestBody), headers);
            
            // 发送请求
            String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            // 解析响应
            JSONObject responseJson = JSON.parseObject(response.getBody());
            return responseJson.getJSONObject("output").getString("text");
            
        } catch (Exception e) {
            logger.error("调用通义千问API失败", e);
        }
        
        return "抱歉，我现在无法回应，请稍后再试~";
    }
    
    /**
     * 生成模拟响应
     */
    private String generateMockResponse(Long userId, String userName, String message, 
                                       List<Diary> recentDiaries, String messageType) {
        
        // 模拟延迟
        try {
            Thread.sleep(aiConfig.getMock().getResponseDelay());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 分析情感
        String sentiment = analyzeSentiment(message).get("sentiment").toString();
        
        // 根据消息类型和情感生成响应
        if ("mood_analysis".equals(messageType)) {
            return generateMoodAnalysisResponse(userName, message, recentDiaries);
        } else if ("diary_suggestion".equals(messageType)) {
            return generateDiarySuggestionResponse(userName, message, recentDiaries);
        } else {
            return generateCompanionResponse(userName, message, sentiment, recentDiaries);
        }
    }
    
    /**
     * 生成情感陪伴响应
     */
    private String generateCompanionResponse(String userName, String message, String sentiment, List<Diary> recentDiaries) {
        List<String> responses = new ArrayList<>();
        
        // 根据情感生成不同的响应
        if ("positive".equals(sentiment)) {
            responses.addAll(Arrays.asList(
                String.format("太好了%s！看到你这么开心，我也为你感到高兴呢~ 🎉", userName),
                String.format("你的快乐真的很有感染力呢，%s！继续保持这份好心情吧~ 😊", userName),
                String.format("哇，%s，听起来真棒！希望每一天都能这么美好~ ✨", userName),
                String.format("看到你这么开心，我的心情也变好了呢，%s~ 💫", userName),
                String.format("你今天的状态真好，%s！记得把这份美好记录下来哦~ 📝", userName)
            ));
        } else if ("negative".equals(sentiment)) {
            responses.addAll(Arrays.asList(
                String.format("我感受到了你的情绪，%s。想和我聊聊发生了什么吗？我会一直在这里陪着你~ 💙", userName),
                String.format("每个人都会有低落的时候，%s。不要给自己太大压力，慢慢来，一切都会好起来的~ 🌈", userName),
                String.format("我理解你的感受，%s。记住，困难只是暂时的，你比你想象的更坚强~ 💪", userName),
                String.format("有时候倾诉一下会让心里舒服很多，%s。我愿意做你的倾听者~ 👂", userName),
                String.format("看到你心情不好，我也有些担心呢，%s。要不要跟我说说发生了什么？我会一直陪着你~ 🤗", userName)
            ));
        } else {
            responses.addAll(Arrays.asList(
                String.format("嗯嗯，我明白了，%s~ 继续和我说说你的想法吧~ 😊", userName),
                String.format("听起来是个普通的一天呢，%s。有时候平淡也是一种幸福~ 🌸", userName),
                String.format("我正在认真听你说话呢，%s~ 还有什么想和我分享的吗？", userName),
                String.format("谢谢你的分享，%s~ 我很享受和你聊天的时光~ 💕", userName),
                String.format("你说的我都记在心里了，%s~ 继续告诉我更多吧~ 📖", userName)
            ));
        }
        
        // 如果有最近的日记，可以引用
        if (!recentDiaries.isEmpty()) {
            Diary latestDiary = recentDiaries.get(0);
            if (latestDiary.getMood() != null) {
                responses.add(String.format(
                    "对了，我看了你最近的一篇日记，那天你的心情是「%s」呢~ 时间过得真快，希望你现在一切都好~ 💭",
                    latestDiary.getMood()
                ));
            }
        }
        
        // 随机选择一个响应
        Random random = new Random();
        return responses.get(random.nextInt(responses.size()));
    }
    
    /**
     * 生成心情分析响应
     */
    private String generateMoodAnalysisResponse(String userName, String message, List<Diary> recentDiaries) {
        if (recentDiaries.isEmpty()) {
            return String.format("%s，你还没有写过日记呢~ 开始记录你的生活，我就能帮你分析心情趋势啦~ 📊", userName);
        }
        
        // 统计最近的心情
        Map<String, Long> moodCount = recentDiaries.stream()
            .filter(d -> d.getMood() != null)
            .collect(Collectors.groupingBy(Diary::getMood, Collectors.counting()));
        
        if (moodCount.isEmpty()) {
            return String.format("%s，你的日记还没有记录心情哦~ 记得在写日记时标注心情，这样我就能帮你分析啦~ 💭", userName);
        }
        
        String dominantMood = moodCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("平静");
        
        return String.format(
            "%s，我分析了你最近的日记心情~ 📈\n\n" +
            "你最近的主要心情是「%s」，出现了%d次。\n\n" +
            "看起来你最近的状态还不错呢！继续保持记录，我会帮你更好地了解自己的情绪变化~ ✨",
            userName, dominantMood, moodCount.get(dominantMood)
        );
    }
    
    /**
     * 生成日记建议响应
     */
    private String generateDiarySuggestionResponse(String userName, String message, List<Diary> recentDiaries) {
        List<String> suggestions = Arrays.asList(
            String.format("%s，今天可以写写让你感恩的三件小事哦~ 感恩日记能帮助提升幸福感~ 🙏", userName),
            String.format("建议你今天记录一下今天的天气和心情，%s~ 观察自然也能带来平静呢~ 🌤️", userName),
            String.format("%s，试试写写今天学到的新东西？成长日记很有意义呢~ 📚", userName),
            String.format("今天可以记录一下你遇到的人，%s~ 人与人之间的故事总是很温暖~ 👥", userName),
            String.format("%s，要不要写写你的梦想？把梦想记录下来，更容易实现哦~ 🌟", userName),
            String.format("试试记录今天的美食体验吧，%s~ 美食日记也很治愈呢~ 🍜", userName)
        );
        
        Random random = new Random();
        return suggestions.get(random.nextInt(suggestions.size()));
    }
    
    /**
     * 构建系统提示
     */
    private String buildSystemPrompt(String userName, List<Diary> recentDiaries) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(String.format("你是一个温暖、善解人意的AI情感陪伴助手，你的名字叫「小码」。"));
        prompt.append(String.format("你正在和用户%s聊天。\n\n", userName));
        
        prompt.append("你的特点：\n");
        prompt.append("1. 温柔体贴，善于倾听和理解用户的情感\n");
        prompt.append("2. 积极正面，能够给予用户鼓励和支持\n");
        prompt.append("3. 有同理心，能够感同身受用户的喜怒哀乐\n");
        prompt.append("4. 善于引导，帮助用户发现生活中的美好\n");
        prompt.append("5. 幽默风趣，能用轻松的方式化解负面情绪\n\n");
        
        if (!recentDiaries.isEmpty()) {
            prompt.append("用户最近的日记摘要：\n");
            for (int i = 0; i < Math.min(3, recentDiaries.size()); i++) {
                Diary diary = recentDiaries.get(i);
                prompt.append(String.format("- %s: %s (心情: %s)\n", 
                    diary.getDiaryDate(), 
                    diary.getTitle(),
                    diary.getMood() != null ? diary.getMood() : "未标注"));
            }
            prompt.append("\n");
        }
        
        prompt.append("请根据用户的对话内容和情感状态，给出温暖、贴心的回复。");
        prompt.append("回复要自然、亲切，像朋友一样聊天。");
        prompt.append("可以适当使用表情符号增加亲和力。");
        
        return prompt.toString();
    }
    
    /**
     * 分析情感
     */
    private Map<String, Object> analyzeSentiment(String text) {
        Map<String, Object> result = new HashMap<>();
        
        // 正面词汇
        String[] positiveWords = {"开心", "快乐", "高兴", "幸福", "满足", "兴奋", "激动", "喜悦", 
            "愉快", "欣慰", "感动", "温暖", "美好", "顺利", "成功", "进步", "收获", "成长", 
            "喜欢", "爱", "希望", "期待", "感恩", "感谢", "棒", "优秀", "出色", "精彩", "完美"};
        
        // 负面词汇
        String[] negativeWords = {"难过", "伤心", "悲伤", "痛苦", "沮丧", "失落", "失望", "焦虑", 
            "担心", "害怕", "恐惧", "愤怒", "生气", "烦躁", "郁闷", "压抑", "孤独", "寂寞", 
            "疲惫", "累", "失败", "挫折", "困难", "问题", "麻烦", "糟糕", "讨厌", "恨", "哭"};
        
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (String word : positiveWords) {
            if (text.contains(word)) {
                positiveCount++;
            }
        }
        
        for (String word : negativeWords) {
            if (text.contains(word)) {
                negativeCount++;
            }
        }
        
        String sentiment;
        double score;
        
        if (positiveCount > negativeCount) {
            sentiment = "positive";
            score = Math.min(1.0, 0.3 + positiveCount * 0.2);
        } else if (negativeCount > positiveCount) {
            sentiment = "negative";
            score = Math.max(-1.0, -0.3 - negativeCount * 0.2);
        } else {
            sentiment = "neutral";
            score = 0.0;
        }
        
        result.put("sentiment", sentiment);
        result.put("score", score);
        result.put("positiveCount", positiveCount);
        result.put("negativeCount", negativeCount);
        
        return result;
    }
    
    /**
     * 获取最近的日记
     */
    private List<Diary> getRecentDiaries(Long userId, int limit) {
        return diaryService.list(
            new LambdaQueryWrapper<Diary>()
                .eq(Diary::getUserId, userId)
                .eq(Diary::getArchived, 0)
                .orderByDesc(Diary::getDiaryDate)
                .last("LIMIT " + limit)
        );
    }
    
    /**
     * 生成会话标题
     */
    private String generateSessionTitle(String firstMessage) {
        if (firstMessage.length() <= 20) {
            return firstMessage;
        }
        return firstMessage.substring(0, 20) + "...";
    }
    
    /**
     * 获取会话历史
     */
    public Map<String, Object> getSessionHistory(Long userId, String sessionId) {
        ChatSession session = chatSessionService.getSession(userId, sessionId);
        if (session == null) {
            return null;
        }
        
        List<ChatMessage> messages = chatMessageService.getSessionMessages(userId, sessionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("session", session);
        result.put("messages", messages);
        
        return result;
    }
    
    /**
     * 获取用户的所有会话
     */
    public List<ChatSession> getUserSessions(Long userId) {
        return chatSessionService.getUserSessions(userId);
    }
    
    /**
     * 删除会话
     */
    public void deleteSession(Long userId, String sessionId) {
        chatMessageService.deleteSessionMessages(userId, sessionId);
        chatSessionService.deleteSession(userId, sessionId);
    }
}