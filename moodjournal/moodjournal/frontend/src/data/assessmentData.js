






export const PHQ9_QUESTIONS = [
  { id: 1, text: "Little interest or pleasure in doing things" },
  { id: 2, text: "Feeling down, depressed, or hopeless" },
  { id: 3, text: "Trouble falling or staying asleep, or sleeping too much" },
  { id: 4, text: "Feeling tired or having little energy" },
  { id: 5, text: "Poor appetite or overeating" },
  { id: 6, text: "Feeling bad about yourself — or that you are a failure or have let yourself or your family down" },
  { id: 7, text: "Trouble concentrating on things, such as reading the newspaper or watching television" },
  { id: 8, text: "Moving or speaking so slowly that other people could have noticed? Or the opposite — being so fidgety or restless that you have been moving around a lot more than usual" },
  { id: 9, text: "Thoughts that you would be better off dead or of hurting yourself in some way" },
];

export const PHQ9_OPTIONS = [
  { value: 0, label: "Not at all" },
  { value: 1, label: "Several days" },
  { value: 2, label: "More than half the days" },
  { value: 3, label: "Nearly every day" },
];




export const BFPT_QUESTIONS = [
  
  { id: 1, text: "Am the life of the party", trait: "extraversion", reversed: false },
  { id: 6, text: "Don't talk a lot", trait: "extraversion", reversed: true },
  { id: 11, text: "Feel comfortable around people", trait: "extraversion", reversed: false },
  { id: 16, text: "Keep in the background", trait: "extraversion", reversed: true },
  { id: 21, text: "Start conversations", trait: "extraversion", reversed: false },
  { id: 26, text: "Have little to say", trait: "extraversion", reversed: true },
  { id: 31, text: "Talk to a lot of different people at parties", trait: "extraversion", reversed: false },
  { id: 36, text: "Don't like to draw attention to myself", trait: "extraversion", reversed: true },
  { id: 41, text: "Don't mind being the center of attention", trait: "extraversion", reversed: false },
  { id: 46, text: "Am quiet around strangers", trait: "extraversion", reversed: true },
  
  
  { id: 2, text: "Feel little concern for others", trait: "agreeableness", reversed: true },
  { id: 7, text: "Am interested in people", trait: "agreeableness", reversed: false },
  { id: 12, text: "Insult people", trait: "agreeableness", reversed: true },
  { id: 17, text: "Sympathize with others' feelings", trait: "agreeableness", reversed: false },
  { id: 22, text: "Am not interested in other people's problems", trait: "agreeableness", reversed: true },
  { id: 27, text: "Have a soft heart", trait: "agreeableness", reversed: false },
  { id: 32, text: "Am not really interested in others", trait: "agreeableness", reversed: true },
  { id: 37, text: "Take time out for others", trait: "agreeableness", reversed: false },
  { id: 42, text: "Feel others' emotions", trait: "agreeableness", reversed: false },
  { id: 47, text: "Make people feel at ease", trait: "agreeableness", reversed: false },
  
  
  { id: 3, text: "Am always prepared", trait: "conscientiousness", reversed: false },
  { id: 8, text: "Leave my belongings around", trait: "conscientiousness", reversed: true },
  { id: 13, text: "Pay attention to details", trait: "conscientiousness", reversed: false },
  { id: 18, text: "Make a mess of things", trait: "conscientiousness", reversed: true },
  { id: 23, text: "Get chores done right away", trait: "conscientiousness", reversed: false },
  { id: 28, text: "Often forget to put things back in their proper place", trait: "conscientiousness", reversed: true },
  { id: 33, text: "Like order", trait: "conscientiousness", reversed: false },
  { id: 38, text: "Shirk my duties", trait: "conscientiousness", reversed: true },
  { id: 43, text: "Follow a schedule", trait: "conscientiousness", reversed: false },
  { id: 48, text: "Am exacting in my work", trait: "conscientiousness", reversed: false },
  
  
  { id: 4, text: "Get stressed out easily", trait: "neuroticism", reversed: false },
  { id: 9, text: "Am relaxed most of the time", trait: "neuroticism", reversed: true },
  { id: 14, text: "Worry about things", trait: "neuroticism", reversed: false },
  { id: 19, text: "Seldom feel blue", trait: "neuroticism", reversed: true },
  { id: 24, text: "Am easily disturbed", trait: "neuroticism", reversed: false },
  { id: 29, text: "Get upset easily", trait: "neuroticism", reversed: false },
  { id: 34, text: "Change my mood a lot", trait: "neuroticism", reversed: false },
  { id: 39, text: "Have frequent mood swings", trait: "neuroticism", reversed: false },
  { id: 44, text: "Get irritated easily", trait: "neuroticism", reversed: false },
  { id: 49, text: "Often feel blue", trait: "neuroticism", reversed: false },
  
  
  { id: 5, text: "Have a rich vocabulary", trait: "openness", reversed: false },
  { id: 10, text: "Have difficulty understanding abstract ideas", trait: "openness", reversed: true },
  { id: 15, text: "Have a vivid imagination", trait: "openness", reversed: false },
  { id: 20, text: "Am not interested in abstract ideas", trait: "openness", reversed: true },
  { id: 25, text: "Have excellent ideas", trait: "openness", reversed: false },
  { id: 30, text: "Do not have a good imagination", trait: "openness", reversed: true },
  { id: 35, text: "Am quick to understand things", trait: "openness", reversed: false },
  { id: 40, text: "Use difficult words", trait: "openness", reversed: false },
  { id: 45, text: "Spend time reflecting on things", trait: "openness", reversed: false },
  { id: 50, text: "Am full of ideas", trait: "openness", reversed: false },
];


export const BFPT_QUESTIONS_SORTED = [...BFPT_QUESTIONS].sort((a, b) => a.id - b.id);

export const BFPT_OPTIONS = [
  { value: 1, label: "Disagree" },
  { value: 2, label: "Slightly disagree" },
  { value: 3, label: "Neutral" },
  { value: 4, label: "Slightly agree" },
  { value: 5, label: "Agree" },
];



export const ENNEAGRAM_QUESTIONS = [
  { id: 1, optionA: { text: "I have been friendly and outgoing", type: 7 }, optionB: { text: "I have been shy and quiet", type: 5 } },
  { id: 2, optionA: { text: "I have been a leader among people", type: 8 }, optionB: { text: "I have been a follower who works well with others", type: 6 } },
  { id: 3, optionA: { text: "I have cared about others' well-being", type: 2 }, optionB: { text: "I have maintained a cool distance from others", type: 5 } },
  { id: 4, optionA: { text: "I have been too controlled and serious", type: 1 }, optionB: { text: "I have been too impulsive and flighty", type: 7 } },
  { id: 5, optionA: { text: "I have been more people-oriented than goal-oriented", type: 2 }, optionB: { text: "I have been more goal-oriented than people-oriented", type: 3 } },
  { id: 6, optionA: { text: "I have been an idealistic person", type: 1 }, optionB: { text: "I have been a pragmatic person", type: 8 } },
  { id: 7, optionA: { text: "I have been self-confident", type: 8 }, optionB: { text: "I have been self-doubting", type: 6 } },
  { id: 8, optionA: { text: "I have been positive and optimistic", type: 7 }, optionB: { text: "I have been intense and pessimistic", type: 4 } },
  { id: 9, optionA: { text: "I have been diplomatic and charming", type: 3 }, optionB: { text: "I have been direct and confrontational", type: 8 } },
  { id: 10, optionA: { text: "I have avoided conflict to maintain peace", type: 9 }, optionB: { text: "I have expressed anger openly when needed", type: 8 } },
  { id: 11, optionA: { text: "I have been a perfectionist about things", type: 1 }, optionB: { text: "I have been relaxed about things", type: 9 } },
  { id: 12, optionA: { text: "I have enjoyed being the center of attention", type: 3 }, optionB: { text: "I have preferred staying in the background", type: 5 } },
  { id: 13, optionA: { text: "I have been able to say 'no' to people", type: 8 }, optionB: { text: "I have had difficulty saying 'no' to people", type: 2 } },
  { id: 14, optionA: { text: "I have been loyal to friends and beliefs", type: 6 }, optionB: { text: "I have been independent and non-conformist", type: 4 } },
  { id: 15, optionA: { text: "I have been focused on my inner world", type: 4 }, optionB: { text: "I have been focused on the outer world", type: 3 } },
  { id: 16, optionA: { text: "I have been competitive", type: 3 }, optionB: { text: "I have been cooperative", type: 9 } },
  { id: 17, optionA: { text: "I have been task-oriented", type: 1 }, optionB: { text: "I have been relationship-oriented", type: 2 } },
  { id: 18, optionA: { text: "I have been concerned about my image", type: 3 }, optionB: { text: "I have not worried about my image", type: 9 } },
  { id: 19, optionA: { text: "I have wanted to be alone", type: 5 }, optionB: { text: "I have wanted to be with others", type: 7 } },
  { id: 20, optionA: { text: "I have been hard on myself", type: 1 }, optionB: { text: "I have been easy on myself", type: 9 } },
  { id: 21, optionA: { text: "I have been reactive and emotional", type: 4 }, optionB: { text: "I have been level-headed and calm", type: 9 } },
  { id: 22, optionA: { text: "I have pursued my personal interests", type: 5 }, optionB: { text: "I have made things happen for others", type: 2 } },
  { id: 23, optionA: { text: "I have been spontaneous and playful", type: 7 }, optionB: { text: "I have been organized and methodical", type: 1 } },
  { id: 24, optionA: { text: "I have wanted to fit in", type: 6 }, optionB: { text: "I have wanted to stand out", type: 4 } },
  { id: 25, optionA: { text: "I have been suspicious of others", type: 6 }, optionB: { text: "I have been trusting of others", type: 9 } },
  { id: 26, optionA: { text: "I have been productive and efficient", type: 3 }, optionB: { text: "I have been laid-back and easygoing", type: 9 } },
  { id: 27, optionA: { text: "I have been imaginative and creative", type: 4 }, optionB: { text: "I have been practical and down-to-earth", type: 6 } },
  { id: 28, optionA: { text: "I have helped others grow", type: 2 }, optionB: { text: "I have protected myself from others", type: 5 } },
  { id: 29, optionA: { text: "I have been restless for new experiences", type: 7 }, optionB: { text: "I have been content with what I have", type: 9 } },
  { id: 30, optionA: { text: "I have been assertive", type: 8 }, optionB: { text: "I have been humble", type: 2 } },
  { id: 31, optionA: { text: "I have expressed my feelings easily", type: 4 }, optionB: { text: "I have kept my feelings to myself", type: 5 } },
  { id: 32, optionA: { text: "I have been adventurous", type: 7 }, optionB: { text: "I have been cautious", type: 6 } },
  { id: 33, optionA: { text: "I have been concerned about doing the right thing", type: 1 }, optionB: { text: "I have been concerned about achieving my goals", type: 3 } },
  { id: 34, optionA: { text: "I have been self-sacrificing", type: 2 }, optionB: { text: "I have been self-protective", type: 8 } },
  { id: 35, optionA: { text: "I have felt different from others", type: 4 }, optionB: { text: "I have felt similar to others", type: 6 } },
  { id: 36, optionA: { text: "I have been intense and serious", type: 4 }, optionB: { text: "I have been light-hearted and fun", type: 7 } },
];






export const EQ60_QUESTIONS = [
  
  { id: 1, text: "I can easily tell if someone else wants to enter a conversation", batch: 1, category: "COGNITIVE", type: "POSITIVE" },
  { id: 2, text: "I prefer animals to humans", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 3, text: "I try to keep up with the current trends and fashions", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 4, text: "I find it difficult to explain to others things that I understand easily, when they don't understand it the first time", batch: 1, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 5, text: "I dream most nights", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 6, text: "I really enjoy caring for other people", batch: 1, category: "COMPASSIONATE", type: "POSITIVE" },
  { id: 7, text: "I try to solve my own problems rather than discussing them with others", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 8, text: "I find it hard to know what to do in a social situation", batch: 1, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 9, text: "I am at my best first thing in the morning", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 10, text: "People often tell me that I went too far in driving my point home in a discussion", batch: 1, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 11, text: "It doesn't bother me too much if I am late meeting a friend", batch: 1, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 12, text: "Friendships and relationships are just too difficult, so I tend not to bother with them", batch: 1, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 13, text: "I would never break a law, no matter how minor", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 14, text: "I often find it difficult to judge if something is rude or polite", batch: 1, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 15, text: "In a conversation, I tend to focus on my own thoughts rather than on what my listener might be thinking", batch: 1, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 16, text: "I prefer practical jokes to verbal humor", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 17, text: "I live life for today rather than the future", batch: 1, category: "DISTRACTOR", type: "NONE" },
  { id: 18, text: "When I was a child, I enjoyed cutting up worms to see what would happen", batch: 1, category: "AFFECTIVE", type: "NEGATIVE" },
  { id: 19, text: "I can pick up quickly if someone says one thing but means another", batch: 1, category: "COGNITIVE", type: "POSITIVE" },
  { id: 20, text: "I tend to have very strong opinions about morality", batch: 1, category: "DISTRACTOR", type: "NONE" },
  
  
  { id: 21, text: "It is hard for me to see why some things upset people so much", batch: 2, category: "AFFECTIVE", type: "NEGATIVE" },
  { id: 22, text: "I find it easy to put myself in somebody else's shoes", batch: 2, category: "COGNITIVE", type: "POSITIVE" },
  { id: 23, text: "I think that good manners are the most important thing a parent can teach their child", batch: 2, category: "DISTRACTOR", type: "NONE" },
  { id: 24, text: "I like to do things on the spur of the moment", batch: 2, category: "DISTRACTOR", type: "NONE" },
  { id: 25, text: "I am good at predicting how someone will feel", batch: 2, category: "COGNITIVE", type: "POSITIVE" },
  { id: 26, text: "I am quick to spot when someone in a group is feeling awkward or uncomfortable", batch: 2, category: "COGNITIVE", type: "POSITIVE" },
  { id: 27, text: "If I say something that someone else is offended by, I think that that's their problem, not mine", batch: 2, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 28, text: "If anyone asked me if I liked their haircut, I would reply truthfully, even if I didn't like it", batch: 2, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 29, text: "I can't always see why someone should have felt offended by a remark", batch: 2, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 30, text: "People often tell me that I am very unpredictable", batch: 2, category: "DISTRACTOR", type: "NONE" },
  { id: 31, text: "I enjoy being the center of attention at any social gathering", batch: 2, category: "DISTRACTOR", type: "NONE" },
  { id: 32, text: "Seeing people cry doesn't really upset me", batch: 2, category: "AFFECTIVE", type: "NEGATIVE" },
  { id: 33, text: "I enjoy having discussions about politics", batch: 2, category: "DISTRACTOR", type: "NONE" },
  { id: 34, text: "I am very blunt, which some people take to be rudeness, even though this is unintentional", batch: 2, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 35, text: "I don't find social situations confusing", batch: 2, category: "COGNITIVE", type: "POSITIVE" },
  { id: 36, text: "Other people tell me I am good at understanding how they are feeling and what they are thinking", batch: 2, category: "COGNITIVE", type: "POSITIVE" },
  { id: 37, text: "When I talk to people, I tend to talk about their experiences rather than my own", batch: 2, category: "COMPASSIONATE", type: "POSITIVE" },
  { id: 38, text: "It upsets me to see an animal in pain", batch: 2, category: "AFFECTIVE", type: "POSITIVE" },
  { id: 39, text: "I am able to make decisions without being influenced by people's feelings", batch: 2, category: "AFFECTIVE", type: "NEGATIVE" },
  { id: 40, text: "I can't relax until I have done everything I had planned to do that day", batch: 2, category: "DISTRACTOR", type: "NONE" },
  
  
  { id: 41, text: "I can easily tell if someone else is interested or bored with what I am saying", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 42, text: "I get upset if I see people suffering on news programs", batch: 3, category: "AFFECTIVE", type: "POSITIVE" },
  { id: 43, text: "Friends usually talk to me about their problems as they say that I am very understanding", batch: 3, category: "COMPASSIONATE", type: "POSITIVE" },
  { id: 44, text: "I can sense if I am intruding, even if the other person doesn't tell me", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 45, text: "I often start new hobbies, but quickly become bored with them and move on to something else", batch: 3, category: "DISTRACTOR", type: "NONE" },
  { id: 46, text: "People sometimes tell me that I have gone too far with teasing", batch: 3, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 47, text: "I would be too nervous to go on a big rollercoaster", batch: 3, category: "DISTRACTOR", type: "NONE" },
  { id: 48, text: "Other people often say that I am insensitive, though I don't always see why", batch: 3, category: "COGNITIVE", type: "NEGATIVE" },
  { id: 49, text: "If I see a stranger in a group, I think that it is up to them to make an effort to join in", batch: 3, category: "COMPASSIONATE", type: "NEGATIVE" },
  { id: 50, text: "I usually stay emotionally detached when watching a film", batch: 3, category: "AFFECTIVE", type: "NEGATIVE" },
  { id: 51, text: "I like to be very organized in day-to-day life and often make lists of the chores I have to do", batch: 3, category: "DISTRACTOR", type: "NONE" },
  { id: 52, text: "I can tune into how someone else feels rapidly and intuitively", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 53, text: "I don't like to take risks", batch: 3, category: "DISTRACTOR", type: "NONE" },
  { id: 54, text: "I can easily work out what another person might want to talk about", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 55, text: "I can tell if someone is masking their true emotion", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 56, text: "Before making a decision, I always weigh up the pros and cons", batch: 3, category: "DISTRACTOR", type: "NONE" },
  { id: 57, text: "I don't consciously work out the rules of social situations", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 58, text: "I am good at predicting what someone will do", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
  { id: 59, text: "I tend to get emotionally involved with a friend's problems", batch: 3, category: "COMPASSIONATE", type: "POSITIVE" },
  { id: 60, text: "I can usually appreciate the other person's viewpoint, even if I don't agree with it", batch: 3, category: "COGNITIVE", type: "POSITIVE" },
];

export const EQ60_OPTIONS = [
  { value: "strongly_agree", label: "Strongly Agree" },
  { value: "slightly_agree", label: "Slightly Agree" },
  { value: "slightly_disagree", label: "Slightly Disagree" },
  { value: "strongly_disagree", label: "Strongly Disagree" },
];


export const getEQBatch = (batchNumber) => {
  return EQ60_QUESTIONS.filter(q => q.batch === batchNumber);
};


export const ASSESSMENT_SECTIONS = [
  { 
    id: 'phq9', 
    name: 'Mental Health Check', 
    description: 'Over the last 2 weeks, how often have you been bothered by the following?',
    questionCount: 9,
    type: 'likert'
  },
  { 
    id: 'tipi', 
    name: 'Personality Profile', 
    description: 'I see myself as:',
    questionCount: 10,
    type: 'likert'
  },
  { 
    id: 'enneagram', 
    name: 'Core Motivations', 
    description: 'For each pair, select the statement that describes you best most of the time.',
    questionCount: 36,
    type: 'paired'
  },
  { 
    id: 'eq', 
    name: 'Empathy Style', 
    description: 'How much do you agree with each statement?',
    questionCount: 20, 
    type: 'likert'
  },
  { 
    id: 'personalized', 
    name: 'Personal Reflection', 
    description: 'Questions tailored to your journal entries.',
    questionCount: 3,
    type: 'text'
  },
];
