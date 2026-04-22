// 咨询师推荐模块的Mock数据（用户端使用）
// 后端未完成时使用这些假数据进行开发

import type {
  RecommendStatus,
  RecommendResult,
  CounselorRecommend,
  CounselorDetail,
  ReviewList,
  CounselorReview,
} from "@/api/counselorRecommend";

// Mock咨询师数据
const mockCounselors: CounselorRecommend[] = [
  {
    id: 101,
    realName: "李明",
    avatar:
      "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 10,
    specialty: ["CBT疗法", "焦虑疏导", "压力管理"],
    rating: 5.0,
    pricePerHour: 600,
    location: "北京",
    nextAvailableTime: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    matchReason: "擅长处理焦虑问题，与您的需求高度匹配",
    tags: ["焦虑症", "压力管理"],
  },
  {
    id: 102,
    realName: "王芳",
    avatar:
      "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 8,
    specialty: ["情绪管理", "家庭关系", "抑郁症"],
    rating: 4.9,
    pricePerHour: 500,
    location: "上海",
    nextAvailableTime: new Date().toISOString(),
    matchReason: "情绪管理专家，适合当前情绪状态",
    tags: ["抑郁症", "情感问题"],
  },
  {
    id: 103,
    realName: "张伟",
    avatar:
      "https://images.unsplash.com/photo-1612349317150-e413f6a5b16d?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 12,
    specialty: ["学业压力", "青少年心理", "考试焦虑"],
    rating: 4.8,
    pricePerHour: 550,
    location: "深圳",
    nextAvailableTime: new Date(
      Date.now() + 3 * 24 * 60 * 60 * 1000
    ).toISOString(),
    matchReason: "学业压力调节专家",
    tags: ["学业压力", "青少年心理"],
  },
  {
    id: 104,
    realName: "刘静",
    avatar:
      "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 9,
    specialty: ["婚恋关系", "职场压力", "情感创伤"],
    rating: 4.9,
    pricePerHour: 580,
    location: "杭州",
    nextAvailableTime: new Date().toISOString(),
    matchReason: "职场压力处理经验丰富",
    tags: ["职场压力", "情感问题"],
  },
  {
    id: 105,
    realName: "陈医生",
    avatar:
      "https://images.unsplash.com/photo-1582750433449-648ed127bb54?w=200&h=200&fit=crop",
    title: "心理学博士",
    experienceYears: 15,
    specialty: ["认知行为疗法", "创伤后应激", "焦虑症"],
    rating: 4.95,
    pricePerHour: 800,
    location: "北京",
    nextAvailableTime: new Date(
      Date.now() + 2 * 24 * 60 * 60 * 1000
    ).toISOString(),
    matchReason: "资深心理学专家",
    tags: ["焦虑症", "压力管理"],
  },
];

// Mock咨询师详情
const mockCounselorDetails: Record<number, CounselorDetail> = {
  101: {
    id: 101,
    realName: "李明",
    avatar:
      "https://images.unsplash.com/photo-1559839734-2b71ea197ec2?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 10,
    specialty: ["CBT疗法", "焦虑疏导", "压力管理"],
    bio: "毕业于北京大学心理学系，从事心理咨询工作10年。专注于认知行为疗法（CBT），帮助来访者识别和改变负面思维模式。擅长处理焦虑、压力相关问题。",
    qualificationUrl: "https://example.com/cert.jpg",
    rating: 5.0,
    reviewCount: 328,
    pricePerHour: 600,
    location: "北京市朝阳区建国路88号",
    isOnline: true,
    tags: ["焦虑症", "压力管理", "CBT疗法"],
  },
  102: {
    id: 102,
    realName: "王芳",
    avatar:
      "https://images.unsplash.com/photo-1594824476967-48c8b964273f?w=200&h=200&fit=crop",
    title: "国家二级心理咨询师",
    experienceYears: 8,
    specialty: ["情绪管理", "家庭关系", "抑郁症"],
    bio: "华东师范大学心理学硕士，8年咨询经验。擅长运用人本主义疗法，帮助来访者探索自我，改善人际关系和家庭沟通。",
    qualificationUrl: "https://example.com/cert2.jpg",
    rating: 4.9,
    reviewCount: 256,
    pricePerHour: 500,
    location: "上海市浦东新区世纪大道100号",
    isOnline: true,
    tags: ["抑郁症", "情感问题", "家庭关系"],
  },
};

// Mock评价数据
const mockReviews: CounselorReview[] = [
  {
    id: 1001,
    userId: 2001,
    nickname: "小明",
    avatar: null,
    rating: 5,
    content: "李医生非常专业，帮助我克服了长期的焦虑问题。非常感谢！",
    createTime: "2024-11-20 14:30:00",
  },
  {
    id: 1002,
    userId: 2002,
    nickname: "心晴",
    avatar: null,
    rating: 5,
    content: "咨询体验很好，医生很有耐心，让我感到被理解和支持。",
    createTime: "2024-11-18 10:15:00",
  },
  {
    id: 1003,
    userId: 2003,
    nickname: "阳光",
    avatar: null,
    rating: 4,
    content: "专业水平很高，给了我很多实用的建议。推荐给有需要的朋友。",
    createTime: "2024-11-15 16:45:00",
  },
];

// Mock数据导出
export const mockCounselorRecommendData = {
  // 获取推荐状态
  getRecommendStatus: (): RecommendStatus => ({
    hasAssessment: true,
    hasMoodLog: true,
    lastAssessmentLevel: "轻度焦虑",
    recommendationReady: true,
  }),

  // 获取推荐咨询师列表
  getRecommendCounselors: (params?: {
    keyword?: string;
    sort?: string;
  }): RecommendResult => {
    let filteredCounselors = [...mockCounselors];

    // 关键词搜索（姓名/专长/标签/地区均模糊匹配）
    if (params?.keyword && params.keyword.trim()) {
      const keyword = params.keyword.toLowerCase();
      filteredCounselors = filteredCounselors.filter(
        (c) =>
          c.realName.toLowerCase().includes(keyword) ||
          c.specialty.some((s) => s.toLowerCase().includes(keyword)) ||
          c.tags.some((t) => t.toLowerCase().includes(keyword)) ||
          (c.location || "").toLowerCase().includes(keyword)
      );
    }

    // 排序
    if (params?.sort === "price_asc") {
      filteredCounselors.sort((a, b) => a.pricePerHour - b.pricePerHour);
    } else if (params?.sort === "rating_desc") {
      filteredCounselors.sort((a, b) => b.rating - a.rating);
    }
    // smart排序保持默认顺序

    return {
      recommendContext: {
        strategy: "smart",
        basedOn: "基于您的心理测评和情绪日记分析",
        userTags: ["压力管理", "焦虑疏导"],
      },
      counselors: filteredCounselors,
    };
  },

  // 获取咨询师详情
  getCounselorDetail: (id: number): CounselorDetail | null => {
    // 如果有预设详情就返回，否则从列表构造
    if (mockCounselorDetails[id]) {
      return mockCounselorDetails[id];
    }
    const counselor = mockCounselors.find((c) => c.id === id);
    if (counselor) {
      return {
        ...counselor,
        bio: `${counselor.realName}是一位经验丰富的心理咨询师，拥有${
          counselor.experienceYears
        }年从业经验，专注于${counselor.specialty.join("、")}等领域。`,
        qualificationUrl: null,
        reviewCount: Math.floor(Math.random() * 200) + 50,
        isOnline: Math.random() > 0.3,
      };
    }
    return null;
  },

  // 获取咨询师评价
  getCounselorReviews: (
    id: number,
    limit: number = 10,
    offset: number = 0
  ): ReviewList => {
    const reviews = mockReviews.slice(offset, offset + limit);
    return {
      total: mockReviews.length,
      avgRating: 4.8,
      reviews,
    };
  },

  // 提交评价
  submitReview: (data: {
    counselorId: number;
    appointmentId: number;
    rating: number;
    content: string;
  }): { reviewId: number } => {
    const newId = Date.now();
    mockReviews.unshift({
      id: newId,
      userId: 1001,
      nickname: "当前用户",
      avatar: null,
      rating: data.rating,
      content: data.content,
      createTime: new Date().toISOString().replace("T", " ").slice(0, 19),
    });
    return { reviewId: newId };
  },
};
