package com.giryong.morningword;

import java.time.LocalDate;

/**
 * 하루 운세 만들기.
 * 그날의 일진(천간·지지)과 태어난 날의 일주를 사주 규칙(십신·합·충)으로 비교해
 * 미리 써 둔 글 중에서 고른다. 재미로 보는 운세이며 미래를 맞히는 것은 아니다.
 */
public final class Fortune {

    public final LocalDate date;
    public final int dayPillar;   // 그날의 일진
    public final int god;         // 십신 번호
    public final int stars;       // 1~5
    public final String keyword, summary, work, money, people, extra, advice;
    public final String luckyColor, luckyNumber, luckyDirection;
    public final int luckyElem;   // 행운의 오행 번호(0~4)

    private Fortune(LocalDate date, int dayPillar, int god, int stars, String keyword, String summary,
                    String work, String money, String people, String extra, String advice,
                    String luckyColor, String luckyNumber, String luckyDirection) {
        this.date = date;
        this.dayPillar = dayPillar;
        this.god = god;
        this.stars = stars;
        this.keyword = keyword;
        this.summary = summary;
        this.work = work;
        this.money = money;
        this.people = people;
        this.extra = extra;
        this.advice = advice;
        this.luckyColor = luckyColor;
        this.luckyNumber = luckyNumber;
        this.luckyDirection = luckyDirection;
        this.luckyElem = java.util.Arrays.asList(COLOR).indexOf(luckyColor);
    }

    /** 별 개수를 짧은 말로 */
    public String scoreWord() {
        return new String[]{"", "조심", "무난", "보통", "좋음", "아주 좋음"}[stars];
    }

    public String starText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) sb.append(i <= stars ? '★' : '☆');
        return sb.toString();
    }

    /** 알림에 넣을 짧은 글 */
    public String shortText() {
        return keyword + " — " + summary;
    }

    // ---------- 계산 ----------

    private static final int[] BASE = {3, 2, 4, 3, 3, 3, 2, 3, 2, 3};

    public static Fortune of(Saju.Person p, LocalDate d) {
        int today = Saju.dayPillar(d);
        int tStem = Saju.stemOf(today);
        int tBranch = Saju.branchOf(today);
        int god = Saju.tenGod(p.dayStem(), tStem);
        int rel = Saju.branchRelation(p.dayBranch(), tBranch);

        int score = BASE[god];
        if (rel == 1 || rel == 2) score++;
        if (rel == -1) score--;
        boolean fills = Saju.stemElem(tStem) == p.weakElem || Saju.branchElem(tBranch) == p.weakElem;
        if (fills) score++;
        // 태어난 시를 알면 시주와 그날 천간의 관계도 조금 반영한다.
        if (p.hourPillar >= 0) {
            int hg = Saju.tenGod(Saju.stemOf(p.hourPillar), tStem);
            if (hg == 9) score++;                   // 정인: 도움
            else if (hg == 6 && score > 1) score--; // 편관: 부담
        }
        score = Math.max(1, Math.min(5, score));

        // 같은 사람·같은 날에는 늘 같은 글이 나오도록 고르는 번호를 정한다.
        long seed = p.solarBirth.toEpochDay() * 7919L + d.toEpochDay() * 104729L
                + (p.timeKnown ? p.hour * 60L + p.minute : 1440);
        int v = (int) Math.floorMod(seed, 997L);

        String[] g = TEXT[god];
        String summary = g[1 + v % 3];
        String work = g[4 + (v / 3) % 2];
        String money = g[6 + (v / 5) % 2];
        String people = g[8 + (v / 7) % 2];

        String extra;
        if (rel == 1) extra = "오늘 날의 기운이 나와 잘 맞아요(합). 사람 사이가 부드러워요.";
        else if (rel == 2) extra = "오늘은 나와 손발이 맞는 날이에요(삼합). 함께 하는 일이 잘 굴러가요.";
        else if (rel == -1) extra = "오늘은 나와 부딪히는 기운이 있어요(충). 운전·이동·말조심만 하면 무난해요.";
        else if (rel == 3) extra = "나와 같은 기운이 겹치는 날이에요. 내 페이스대로 가면 돼요.";
        else extra = "";
        if (fills) {
            String f = "나에게 부족한 " + Saju.ELEM[p.weakElem] + "(" + Saju.ELEM_H[p.weakElem]
                    + ") 기운이 들어와 힘이 채워지는 날이에요.";
            extra = extra.isEmpty() ? f : extra + "\n" + f;
        }

        int w = p.weakElem;
        return new Fortune(d, today, god, score, g[0], summary, work, money, people, extra, g[10],
                COLOR[w], NUMBER[w], DIRECTION[w]);
    }

    private static final String[] COLOR = {"초록색", "빨간색", "노란색", "흰색", "검정·남색"};
    private static final String[] NUMBER = {"3, 8", "2, 7", "5, 10", "4, 9", "1, 6"};
    private static final String[] DIRECTION = {"동쪽", "남쪽", "가운데", "서쪽", "북쪽"};

    /**
     * 십신별 글. {키워드, 총평1, 총평2, 총평3, 일1, 일2, 돈1, 돈2, 사람1, 사람2, 한마디}
     */
    static final String[][] TEXT = {
        { // 0 비견: 나와 같은 기운
            "내 편이 생기는 날",
            "나와 닮은 기운이 들어오는 날이에요. 혼자 끙끙대기보다 동료와 나누면 일이 가벼워져요.",
            "자신감이 올라가는 날이에요. 미뤄 둔 일을 밀고 나가기 좋아요.",
            "고집이 조금 세질 수 있어요. 내 생각이 맞아도 한 번은 남의 말을 들어 보세요.",
            "함께 하는 일에서 성과가 나요. 역할을 나누면 빨라져요.",
            "내 방식대로 밀고 가도 괜찮은 날이에요.",
            "큰 지출보다는 지키는 쪽이 좋아요.",
            "같이 내는 돈(회비·나눠 내기)은 미리 정리해 두세요.",
            "오래된 동료나 친구에게 연락이 올 수 있어요.",
            "편한 사람과 점심을 함께 해 보세요.",
            "같이 가면 멀리 가요."
        },
        { // 1 겁재: 경쟁
            "욕심은 반만 내는 날",
            "경쟁하는 기운이 있는 날이에요. 이기려고 애쓰기보다 내 속도를 지키세요.",
            "기운이 넘쳐 서두르기 쉬워요. 결정은 한 번 더 확인하고 내리세요.",
            "남과 비교가 되는 날이에요. 비교는 어제의 나하고만 하세요.",
            "급하게 처리한 일에서 실수가 나기 쉬워요. 숫자·날짜를 한 번 더 보세요.",
            "내 몫과 남의 몫을 분명히 해 두면 뒤탈이 없어요.",
            "충동구매 주의! 장바구니에 하루 묵혀 두세요.",
            "돈을 빌려주는 일은 오늘은 피하는 게 좋아요.",
            "말다툼이 생기면 먼저 웃으며 한발 물러서세요.",
            "경쟁자에게도 배울 점이 있어요.",
            "천천히 해도 늦지 않아요."
        },
        { // 2 식신: 즐거움·먹을 복
            "먹을 복·웃을 일 있는 날",
            "마음이 여유롭고 표정이 밝아지는 날이에요. 맛있는 점심이 기다려요.",
            "아이디어가 술술 나오는 날이에요. 떠오른 생각은 바로 메모해 두세요.",
            "하는 일마다 재미가 붙는 날이에요. 즐겁게 하면 결과도 좋아요.",
            "기획·글쓰기·정리 같은 일이 잘 풀려요.",
            "꾸준히 해 온 일이 칭찬받을 수 있어요.",
            "작은 즐거움에 쓰는 돈은 아깝지 않은 날이에요.",
            "먹을 것이 생기거나 대접받을 일이 있어요.",
            "내가 먼저 웃으면 분위기가 확 살아나요.",
            "간식을 나누면 사이가 더 좋아져요.",
            "오늘은 즐기는 사람이 이겨요."
        },
        { // 3 상관: 말·재능
            "말 한마디가 중요한 날",
            "재치와 말솜씨가 살아나는 날이에요. 다만 윗사람 앞에서는 말을 아껴 주세요.",
            "답답한 걸 바꾸고 싶어지는 날이에요. 바꾸자고 할 때는 근거를 함께 준비하세요.",
            "생각이 빨리 돌아가는 날이에요. 말이 손보다 앞서지 않게 조심하세요.",
            "개선 아이디어를 내기 좋아요. 문서로 정리해서 내면 더 잘 통해요.",
            "보고할 때는 결론부터 짧게 말하세요.",
            "기분 따라 쓰는 돈이 생기기 쉬워요.",
            "재능을 살린 부수입 아이디어가 떠오를 수 있어요.",
            "농담이 오해가 되지 않게 한 번 더 생각하고 말하세요.",
            "칭찬 한마디가 오늘의 행운을 불러요.",
            "좋은 말은 크게, 불평은 작게."
        },
        { // 4 편재: 기회·활동
            "기회가 오는 날",
            "움직이는 만큼 얻는 날이에요. 밖에 나가 사람을 만나면 좋은 소식이 있어요.",
            "뜻밖의 기회가 보이는 날이에요. 다만 너무 큰 욕심은 조금 덜어 내세요.",
            "일이 여러 갈래로 바빠지는 날이에요. 중요한 순서부터 하나씩 하세요.",
            "외근·미팅·협상에서 성과가 나요.",
            "새로 맡는 일이 생각보다 괜찮아요.",
            "받을 돈이 있으면 오늘 챙겨 보세요.",
            "투자나 큰 거래는 서두르지 말고 따져 보고 하세요.",
            "새로운 사람과 인연이 닿을 수 있어요.",
            "모임에서 분위기를 띄우면 득이 돼요.",
            "발로 뛰면 복이 따라와요."
        },
        { // 5 정재: 성실·안정
            "차곡차곡 쌓이는 날",
            "성실함이 보답받는 날이에요. 꼼꼼하게 한 일이 빛을 봐요.",
            "안정된 기운이 흐르는 날이에요. 계획대로 하면 무리 없이 끝나요.",
            "작은 것을 아끼고 챙기면 크게 돌아오는 날이에요.",
            "정리·정산·서류 일이 잘 맞아요.",
            "마감이 있는 일을 끝내기 좋은 날이에요.",
            "지출을 정리하기 좋은 날이에요.",
            "작은 돈이 모여 기분 좋은 숫자가 돼요.",
            "약속을 잘 지키면 믿음이 커져요.",
            "가족에게 작은 선물을 해 보세요.",
            "꾸준함이 최고의 재테크예요."
        },
        { // 6 편관: 부담·책임
            "버티면 이기는 날",
            "할 일이 몰리고 부담이 느껴질 수 있어요. 하나씩 끝내면 저녁엔 뿌듯해요.",
            "책임질 일이 생기는 날이에요. 피하지 않으면 실력으로 인정받아요.",
            "몸과 마음이 조금 지칠 수 있어요. 중간중간 쉬는 시간을 꼭 챙기세요.",
            "급한 지시가 내려올 수 있어요. 할 일 순서를 다시 정하세요.",
            "어려운 일을 맡으면 오히려 기회가 돼요.",
            "예상 못 한 지출이 생길 수 있어요. 여유 돈을 남겨 두세요.",
            "내야 할 돈의 마감일을 오늘 미리 확인하세요.",
            "윗사람 말은 일단 끝까지 들어 보세요.",
            "날이 선 말에는 대꾸보다 웃음이 약이에요.",
            "오늘의 무게가 내일의 근육이 돼요."
        },
        { // 7 정관: 인정·원칙
            "인정받는 날",
            "반듯한 기운이 도는 날이에요. 원칙대로 하면 칭찬이 따라와요.",
            "윗사람에게 좋은 평가를 받을 수 있는 날이에요. 보고는 오늘 해 보세요.",
            "맡은 자리에서 믿음을 얻는 날이에요. 차분하게 하면 돼요.",
            "결재·보고·회의가 순조로워요.",
            "규정과 절차를 지키면 일이 매끄러워요.",
            "월급처럼 정해진 돈이 든든하게 느껴지는 날이에요.",
            "공식 서류·계약은 오늘 처리하면 좋아요.",
            "예의 바른 한마디가 큰 점수가 돼요.",
            "도움을 청하면 들어줄 사람이 있어요.",
            "바른 길이 빠른 길이에요."
        },
        { // 8 편인: 생각·공부
            "생각이 깊어지는 날",
            "혼자 생각하는 시간이 필요한 날이에요. 조용한 곳에서 일하면 집중이 잘 돼요.",
            "남들이 못 보는 걸 보는 날이에요. 떠오른 느낌을 메모해 두세요.",
            "마음이 이랬다저랬다 할 수 있어요. 중요한 결정은 오후에 하는 게 좋아요.",
            "자료 조사·공부·새 프로그램 배우기에 좋아요.",
            "계획을 바꾸고 싶어지면 이유부터 적어 보세요.",
            "안 쓰는 구독·자동결제를 정리하기 좋은 날이에요.",
            "배우는 데 쓰는 돈은 남는 장사예요.",
            "말수가 줄 수 있어요. 오해 없게 표정은 밝게!",
            "혼자 먹는 점심도 괜찮은 날이에요.",
            "조용한 시간이 답을 줘요."
        },
        { // 9 정인: 도움·보호
            "도움받는 날",
            "든든한 도움이 들어오는 날이에요. 막히면 혼자 고민 말고 물어보세요.",
            "마음이 편안하고 안정되는 날이에요. 차분히 하면 다 잘 풀려요.",
            "좋은 소식이나 반가운 서류가 올 수 있는 날이에요.",
            "선배·상사의 도움으로 일이 쉽게 풀려요.",
            "문서·계약·자격 관련 일이 잘 돼요.",
            "급하게 벌기보다 지키기 좋은 날이에요.",
            "누군가 챙겨 주는 작은 혜택이 있어요.",
            "부모님이나 어른께 안부 전화 한 통 해 보세요.",
            "고맙다는 말을 하면 더 큰 도움이 와요.",
            "기대도 괜찮아요. 오늘은 받는 날이에요."
        }
    };
}
