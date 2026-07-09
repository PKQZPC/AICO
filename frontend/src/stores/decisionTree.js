import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { decisionTreeApi } from '@/api/decisionTree';

// 静态决策树数据
const staticTrees = {
  anxiety_management_v1: {
    "tree_id": "anxiety_management_v1",
    "tree_name": "通用焦虑管理决策树",
    "root": {
      "node_id": "node_root",
      "node_type": "decision",
      "label": "焦虑症状初筛"
    },
    "node_registry": [
      {
        "node_id": "node_root",
        "node_type": "decision",
        "label": "焦虑症状初筛",
        "status": "to_be_confirm",
        "node_content": "评估用户是否表现出焦虑症状",
        "inference_relationship": "初步判断是否需要进一步评估焦虑严重程度",
        "children": [
          {
            "relationship": "solid",
            "node_id": "node_symptom_details"
          },
          {
            "relationship": "dashed",
            "node_id": "node_exit"
          }
        ]
      },
      {
        "node_id": "node_symptom_details",
        "node_type": "decision",
        "label": "症状详情收集",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？",
        "inference_relationship": "具体症状和持续时间是评估焦虑等级的关键指标",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "用户提供详情",
            "node_id": "node_severity_assessment"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户拒绝回答",
            "node_id": "node_exit"
          }
        ]
      },
      {
        "node_id": "node_severity_assessment",
        "node_type": "decision",
        "label": "严重程度评估",
        "node_content": "评估焦虑的严重程度",
        "inference_relationship": "根据症状表现和持续时间评估焦虑等级",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "高焦虑",
            "node_id": "node_high_severity"
          },
          {
            "relationship": "solid",
            "edge_label": "中焦虑",
            "node_id": "node_medium_severity"
          },
          {
            "relationship": "solid",
            "edge_label": "低焦虑",
            "node_id": "node_low_severity"
          }
        ]
      },
      {
        "node_id": "node_high_severity",
        "node_type": "leaf",
        "label": "高焦虑等级",
        "node_content": "建议寻求专业心理咨询",
        "response_template": "您的症状可能需要专业支持，建议联系心理咨询师。"
      },
      {
        "node_id": "node_medium_severity",
        "node_type": "leaf",
        "label": "中焦虑等级",
        "node_content": "提供焦虑管理策略",
        "response_template": "您可能经历中度焦虑，这里有一些管理焦虑的有效策略..."
      },
      {
        "node_id": "node_low_severity",
        "node_type": "leaf",
        "label": "低焦虑等级",
        "node_content": "提供日常应对建议",
        "response_template": "您的焦虑程度较轻，这些简单的日常习惯可能有所帮助..."
      },
      {
        "node_id": "node_exit",
        "node_type": "leaf",
        "label": "结束对话",
        "node_content": "尊重用户选择，结束评估",
        "response_template": "感谢您的分享，如果未来有需要，随时可以重新开始评估。"
      }
    ],
    "metadata": {
      "version": "2.0.0",
      "generate_time": "2025-04-11T00:00:00+08:00",
      "design_scope": {
        "applicable_scenarios": ["在线咨询", "自评工具", "客服系统"],
        "excluded_scenarios": ["紧急危机干预"]
      },
      "author": "clinical_design_team"
    }
  },
  anxiety_management_pro_v3: {
    "tree_id": "anxiety_management_pro_v3",
    "tree_name": "通用焦虑评估决策树（含拒绝逻辑）",
    "root": {
      "node_id": "node_root",
      "node_type": "decision",
      "label": "评估入口"
    },
    "node_registry": [
      {
        "node_id": "node_root",
        "node_type": "decision",
        "label": "评估入口",
        "status": "to_be_confirm",
        "node_content": "获取用户评估授权",
        "trigger_condition": "当用户提及焦虑关键词时触发",
        "prompt_template": "我们有一个标准化评估工具，您是否愿意尝试？",
        "inference_relationship": "伦理要求：必须获得初始授权",
        "children": [
          {
            "relationship": "solid",
            "node_id": "node_assess_severity"
          },
          {
            "relationship": "dashed",
            "node_id": "node_immediate_reject"
          }
        ]
      },
      {
        "node_id": "node_immediate_reject",
        "node_type": "leaf",
        "label": "初始拒绝",
        "response_template": "尊重您的选择，这是24小时心理援助热线：400-161-9995",
        "metadata": {
          "risk_level": "low",
          "action": "记录拒绝原因到日志"
        }
      },
      {
        "node_id": "node_assess_severity",
        "node_type": "decision",
        "label": "分级评估",
        "node_content": "根据评分规则分级",
        "evaluation_rules": [
          {
            "condition": "score <= 4",
            "target_node": "node_mild",
            "description": "轻度焦虑"
          },
          {
            "condition": "5 <= score <= 7",
            "target_node": "node_moderate",
            "description": "中度焦虑"
          },
          {
            "condition": "score >= 8",
            "target_node": "node_severe",
            "description": "重度焦虑"
          }
        ],
        "exit_path": {
          "node_id": "node_dropout",
          "label": "评估中途退出"
        },
        "children": [
          {
            "relationship": "solid",
            "edge_label": "轻度",
            "node_id": "node_mild"
          },
          {
            "relationship": "solid",
            "edge_label": "中度",
            "node_id": "node_moderate"
          },
          {
            "relationship": "solid",
            "edge_label": "重度",
            "node_id": "node_severe"
          },
          {
            "relationship": "dashed",
            "edge_label": "退出",
            "node_id": "node_dropout"
          }
        ]
      },
      {
        "node_id": "node_mild",
        "node_type": "decision",
        "label": "轻度干预",
        "node_content": "提供轻度焦虑的干预方案",
        "recommendation_template": "推荐使用冥想指南，您需要具体指导吗？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "接受",
            "node_id": "node_mild_accept"
          },
          {
            "relationship": "dashed",
            "edge_label": "拒绝",
            "node_id": "node_mild_reject"
          }
        ]
      },
      {
        "node_id": "node_moderate",
        "node_type": "leaf",
        "label": "中度干预",
        "node_content": "提供中度焦虑的干预方案",
        "response_template": "您的焦虑症状达到中度水平，建议采用认知行为疗法技术。"
      },
      {
        "node_id": "node_severe",
        "node_type": "decision",
        "label": "危机评估",
        "node_content": "评估用户是否存在安全风险",
        "mandatory_disclaimer": "回答不会触发自动干预",
        "risk_assessment_rules": [
          {
            "question_template": "您最近是否有伤害自己的想法？",
            "yes_node": "node_emergency",
            "no_node": "node_severe_monitor",
            "decline_node": "node_severe_reject"
          }
        ],
        "children": [
          {
            "relationship": "solid",
            "edge_label": "有风险",
            "node_id": "node_emergency"
          },
          {
            "relationship": "solid",
            "edge_label": "无风险",
            "node_id": "node_severe_monitor"
          },
          {
            "relationship": "dashed",
            "edge_label": "拒绝回答",
            "node_id": "node_severe_reject"
          }
        ]
      },
      {
        "node_id": "node_mild_accept",
        "node_type": "leaf",
        "label": "接受自助",
        "node_content": "提供详细的自助指导",
        "response_template": "很好，这里是详细的冥想指南和练习步骤..."
      },
      {
        "node_id": "node_mild_reject",
        "node_type": "leaf",
        "label": "拒绝自助",
        "node_content": "提供替代资源",
        "response_template": "已为您保存资源链接供后续使用",
        "metadata": {
          "follow_up": "3天后发送提醒"
        }
      },
      {
        "node_id": "node_emergency",
        "node_type": "leaf",
        "label": "紧急协议",
        "node_content": "启动紧急干预流程",
        "action_flow": [
          "触发危机协议",
          "通知专业人员"
        ],
        "response_template": "您的安全对我们非常重要，我们将立即联系专业人员为您提供支持。"
      },
      {
        "node_id": "node_severe_monitor",
        "node_type": "leaf",
        "label": "重度监控",
        "node_content": "提供专业治疗建议并安排随访",
        "response_template": "您的症状需要专业治疗，建议尽快联系心理医生，我们会在一周后随访。"
      },
      {
        "node_id": "node_severe_reject",
        "node_type": "leaf",
        "label": "风险拒绝",
        "node_content": "尊重拒绝但提供紧急联系方式",
        "response_template": "尊重您的选择，如情况变化，这里是紧急联系方式：400-161-9995。"
      },
      {
        "node_id": "node_dropout",
        "node_type": "leaf",
        "label": "评估退出",
        "node_content": "记录中断原因并提供后续选项",
        "response_template": "您可以随时继续评估，或者尝试其他方式获取支持。"
      }
    ],
    "metadata": {
      "version": "3.1.0",
      "design_scope": {
        "applicable_scenarios": ["在线咨询", "移动健康APP"],
        "compliance": ["HIPAA", "精神卫生法"]
      },
      "variables": {
        "emergency_contact": "400-161-9995",
        "self_help_tool": ["冥想指南", "焦虑日记模板"],
        "risk_behavior": ["伤害自己", "自杀"]
      }
    }
  },
  homework: {
    "tree_id": "homework",
    "tree_name": "作业决策树",
    
    "root": {
      "node_id": "node_root",
      "node_type": "decision",
      "label": "作业问题"
    },
    
    "node_registry": [
      {
        "node_id": "node_root",
        "node_type": "decision",
        "label": "作业问题",
        "status": "to_be_confirm",
        "source_content": "生成的上下文内容",
        "node_content": "评估用户是否表现出焦虑症状",
        "inference_relationship": "初步判断是否需要进一步评估焦虑严重程度",
        
        "children": [
          {
            "relationship": "solid",
            "node_id": "2.1"
          },
          {
            "relationship": "dashed",
            "node_id": "2.2"
          },
          {
            "relationship": "dashed",
            "node_id": "2.3"
          }
        ]
      },
      
      {
        "node_id": "2.1",
        "node_type": "decision",
        "label": "写作业拖拉磨蹭怎么办",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？",
        "inference_relationship": "具体症状和持续时间是评估焦虑等级的关键指标",
        
        "children": [
          {
            "relationship": "solid",
            "edge_label": "用户提供详情",
            "node_id": "2.1.1"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.2"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.3"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.4"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.5"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.6"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.7"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.8"
          }
        ]
      },
      {
        "node_id": "2.1.1",
        "node_type": "decision",
        "label": "儿童天生如此，反应性低，社会抑制性强",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.2",
        "node_type": "decision",
        "label": "你妈觉得你蹭",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.3",
        "node_type": "decision",
        "label": "父母错误行为导致儿童用蹭来对付",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.4",
        "node_type": "decision",
        "label": "学习基础差,对学习没自信,没兴趣，没成就感",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.5",
        "node_type": "decision",
        "label": "时间观念差",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？",
        "inference_relationship": "具体症状和持续时间是评估焦虑等级的关键指标",
        
        "children": [
          {
            "relationship": "solid",
            "edge_label": "用户提供详情",
            "node_id": "2.1.5.1"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.5.2"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.1.5.3"
          }
        ]
      },
      {
        "node_id": "2.1.5.1",
        "node_type": "decision",
        "label": "寒暑假定时间计划",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.5.2",
        "node_type": "decision",
        "label": "一分钟专项训练",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.5.3",
        "node_type": "decision",
        "label": "使用番茄钟",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.6",
        "node_type": "decision",
        "label": "条理性差,写作业没顺序",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.7",
        "node_type": "decision",
        "label": "动机不足，没成就感;",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.1.8",
        "node_type": "decision",
        "label": "家庭环境有问题;",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.2",
        "node_type": "decision",
        "label": "写作业畏难怎么办",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？",
        "inference_relationship": "具体症状和持续时间是评估焦虑等级的关键指标",
        
        "children": [
          {
            "relationship": "solid",
            "edge_label": "用户提供详情",
            "node_id": "2.2.1"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.2.2"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.2.3"
          }
        ]
      },
      {
        "node_id": "2.2.1",
        "node_type": "decision",
        "label": "存在习得性无助-一合理要求",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.2.2",
        "node_type": "decision",
        "label": "无法开始--小步子得成绩",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.2.3",
        "node_type": "decision",
        "label": "缺乏正确的沟通方式",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.3",
        "node_type": "decision",
        "label": "写作业专注力不行怎么办?",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？",
        "inference_relationship": "具体症状和持续时间是评估焦虑等级的关键指标",
        
        "children": [
          {
            "relationship": "solid",
            "edge_label": "用户提供详情",
            "node_id": "2.3.1"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.3.2"
          },
          {
            "relationship": "dashed",
            "edge_label": "用户提供详情",
            "node_id": "2.3.3"
          }
        ]
      },
      {
        "node_id": "2.3.1",
        "node_type": "decision",
        "label": "学习环境不好:扫除障碍法",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.3.2",
        "node_type": "decision",
        "label": "中间会休息吗?中途休息法",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      },
      {
        "node_id": "2.3.3",
        "node_type": "decision",
        "label": "增强鼓励减少批评，恰当沟通:语气信任法",
        "node_content": "询问症状的具体表现和持续时间",
        "prompt_template": "您能否描述症状的具体表现？这些症状持续了多久？"
      }
    ],
    
    "metadata": {
      "version": "2.0.0",
      "generate_time": "2025-04-11T00:00:00+08:00",
      "design_scope": {
        "applicable_scenarios": ["在线咨询", "自评工具", "客服系统"],
        "excluded_scenarios": ["紧急危机干预"]
      },
      "author": "clinical_design_team"
    }
  },
  // 新增的压力咨询决策树
  stress: {
    "tree_id": "stress",
    "tree_name": "大学生/考生压力咨询决策树",
    "root": {
      "node_id": "root",
      "node_type": "decision",
      "label": "压力咨询入口"
    },
    "node_registry": [
      {
        "node_id": "root",
        "node_type": "decision",
        "label": "压力咨询入口",
        "status": "confirmed",
        "node_content": "压力咨询主题选择",
        "source_content": "专家确认",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "考试焦虑",
            "node_id": "exam_anxiety"
          },
          {
            "relationship": "solid",
            "edge_label": "生理症状",
            "node_id": "physical_symptoms"
          },
          {
            "relationship": "solid",
            "edge_label": "学习拖延",
            "node_id": "procrastination"
          }
        ]
      },
      {
        "node_id": "exam_anxiety",
        "node_type": "decision",
        "label": "对考试失败的过度担忧或灾难化思维",
        "status": "confirmed",
        "node_content": "负面思维模式是压力的重要来源",
        "source_content": "专家确认",
        "prompt_template": "您对考试失败最担心的是什么？能具体描述一下吗？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "人生无望",
            "node_id": "no_hope"
          },
          {
            "relationship": "solid",
            "edge_label": "辜负期望",
            "node_id": "expectations"
          },
          {
            "relationship": "solid",
            "edge_label": "否定价值",
            "node_id": "self_value"
          }
        ]
      },
      {
        "node_id": "no_hope",
        "node_type": "decision",
        "label": "认为如果考试失败，人生就没有希望了",
        "status": "confirmed",
        "node_content": "识别核心非理性信念",
        "source_content": "专家确认",
        "prompt_template": "这种想法对您有什么影响？您觉得这种想法出现的频率如何？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "最坏结果探讨",
            "node_id": "worst_case"
          }
        ]
      },
      {
        "node_id": "worst_case",
        "node_type": "leaf",
        "label": "引导来访思考：如果这次考试失败了，最坏的结果具体是什么？探讨应对策略和B计划",
        "status": "confirmed",
        "node_content": "帮助来访者建立更现实的预期和应对感",
        "source_content": "专家确认",
        "response_template": "让我们一起看看，如果真的发生了最坏的情况，我们可以做些什么来应对？"
      },
      {
        "node_id": "expectations",
        "node_type": "decision",
        "label": "担心考试失败会辜负父母/他人的期望",
        "status": "confirmed",
        "node_content": "区分内在动机和外在压力",
        "source_content": "专家确认",
        "prompt_template": "您觉得这些期望主要来自哪里？这对您来说意味着什么？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "区分期望",
            "node_id": "distinguish_expectations"
          }
        ]
      },
      {
        "node_id": "distinguish_expectations",
        "node_type": "leaf",
        "label": "引导来访区分自身目标与他人期望，探讨沟通方式以管理外部压力",
        "status": "confirmed",
        "node_content": "增强自主感，减少外部压力内化",
        "source_content": "专家确认",
        "response_template": "您希望别人如何理解您的压力？您觉得可以怎样和他们沟通您的感受？"
      },
      {
        "node_id": "self_value",
        "node_type": "decision",
        "label": "感觉一次失败就否定了自己所有努力和价值",
        "status": "confirmed",
        "node_content": "识别条件性自我价值感问题",
        "source_content": "专家确认",
        "prompt_template": "您认为考试成绩在多大程度上代表了您的个人价值？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "价值认同",
            "node_id": "value_recognition"
          }
        ]
      },
      {
        "node_id": "value_recognition",
        "node_type": "leaf",
        "label": "引导来访挑战非黑即白思维，认识到努力过程的价值，将考试结果与个人核心价值分离",
        "status": "confirmed",
        "node_content": "提升无条件的自我接纳",
        "source_content": "专家确认",
        "response_template": "除了学习成绩，您认为自己还有哪些重要的品质和价值？这些价值是如何体现的？"
      },
      {
        "node_id": "physical_symptoms",
        "node_type": "decision",
        "label": "出现明显的考试焦虑症状（生理/心理）",
        "status": "confirmed",
        "node_content": "了解症状有助于选择合适的应对技巧",
        "source_content": "专家确认",
        "prompt_template": "您在考试前后或期间会经历哪些不适感（比如心慌、紧张、失眠、注意力不集中）？这些情况有多频繁，多严重？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "身体不适",
            "node_id": "body_discomfort"
          },
          {
            "relationship": "solid",
            "edge_label": "思绪纷乱",
            "node_id": "thoughts_disorder"
          },
          {
            "relationship": "solid",
            "edge_label": "生活影响",
            "node_id": "life_impact"
          }
        ]
      },
      {
        "node_id": "body_discomfort",
        "node_type": "decision",
        "label": "考试时或考前感到心慌、手抖、出汗、肠胃不适、头脑空白",
        "status": "confirmed",
        "node_content": "生理症状是焦虑的直接体现",
        "source_content": "专家确认",
        "prompt_template": "这些身体反应在什么时候最明显？它们对您集中注意力或答题有影响吗？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "放松技巧",
            "node_id": "relaxation"
          }
        ]
      },
      {
        "node_id": "relaxation",
        "node_type": "leaf",
        "label": "教授放松技巧（如腹式深呼吸、渐进式肌肉放松），讨论考场应对策略（如暂停、积极自我对话）",
        "status": "confirmed",
        "node_content": "直接干预生理症状，恢复平静状态",
        "source_content": "专家确认",
        "response_template": "我们可以练习一些简单的放松方法，您愿意试试吗？"
      },
      {
        "node_id": "thoughts_disorder",
        "node_type": "decision",
        "label": "过度担忧、思绪纷乱、无法集中精力复习或考试",
        "status": "confirmed",
        "node_content": "认知层面的焦虑表现",
        "source_content": "专家确认",
        "prompt_template": "当您尝试复习或考试时，脑子里通常会想些什么？这些想法如何影响您的专注力？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "思维技术",
            "node_id": "thought_techniques"
          }
        ]
      },
      {
        "node_id": "thought_techniques",
        "node_type": "leaf",
        "label": "运用思维打断技术、正念练习，制定结构化的复习计划，练习专注力训练",
        "status": "confirmed",
        "node_content": "改善认知功能，提高学习和考试效率",
        "source_content": "专家确认",
        "response_template": "当担忧的想法出现时，尝试用一个'停'字打断它，然后把注意力转回到当下的任务上。我们可以一起练习一下。"
      },
      {
        "node_id": "life_impact",
        "node_type": "decision",
        "label": "考前失眠、食欲不振/暴饮暴食、易怒或情绪低落",
        "status": "confirmed",
        "node_content": "焦虑影响整体身心状态",
        "source_content": "专家确认",
        "prompt_template": "最近您的睡眠和饮食习惯有变化吗？您的情绪状态如何？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "生活规律",
            "node_id": "lifestyle"
          }
        ]
      },
      {
        "node_id": "lifestyle",
        "node_type": "leaf",
        "label": "探讨睡眠卫生、规律作息、健康饮食、适度运动的重要性，推荐压力管理活动",
        "status": "confirmed",
        "node_content": "从基础生活层面改善应对压力的能力",
        "source_content": "专家确认",
        "response_template": "保持规律的生活习惯对缓解压力很有帮助。您觉得可以在哪些方面做些调整？"
      },
      {
        "node_id": "procrastination",
        "node_type": "decision",
        "label": "复习拖延、缺乏动力、逃避学习或感到不知所措",
        "status": "confirmed",
        "node_content": "行为问题往往与潜在的心理因素相关",
        "source_content": "专家确认",
        "prompt_template": "您在开始复习或坚持复习时遇到了哪些困难？您通常会怎么做？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "不知所措",
            "node_id": "overwhelmed"
          },
          {
            "relationship": "solid",
            "edge_label": "完美主义",
            "node_id": "perfectionism"
          },
          {
            "relationship": "solid",
            "edge_label": "缺乏兴趣",
            "node_id": "lack_of_interest"
          }
        ]
      },
      {
        "node_id": "overwhelmed",
        "node_type": "decision",
        "label": "觉得复习内容太多，不知从何开始，导致拖延",
        "status": "confirmed",
        "node_content": "\"不知所措\"是常见的拖延触发器",
        "source_content": "专家确认",
        "prompt_template": "面对庞大的复习任务，您有什么感受？您通常如何规划复习？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "任务分解",
            "node_id": "task_breakdown"
          }
        ]
      },
      {
        "node_id": "task_breakdown",
        "node_type": "leaf",
        "label": "指导任务分解技巧,制定具体的、可操作的、分阶段的小目标(SMART原则),从易到难",
        "status": "confirmed",
        "node_content": "降低启动门槛，通过小成功建立信心",
        "source_content": "专家确认",
        "response_template": "我们可以试着把一个大的复习章节分解成几个小部分，先完成一个小部分感觉怎么样？"
      },
      {
        "node_id": "perfectionism",
        "node_type": "decision",
        "label": "因追求完美而迟迟不动手，或过度准备、反复修改，效率低下",
        "status": "confirmed",
        "node_content": "功能失调性完美主义常导致拖延和焦虑",
        "source_content": "专家确认",
        "prompt_template": "您是否对自己的复习或考试表现有非常高的标准？达不到标准时会怎样？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "调整认知",
            "node_id": "adjust_perfectionism"
          }
        ]
      },
      {
        "node_id": "adjust_perfectionism",
        "node_type": "leaf",
        "label": "调整完美主义观念，接受\"足够好\"原则，强调完成任务的重要性，设定时间限制（番茄工作法）",
        "status": "confirmed",
        "node_content": "打破完美主义驱动的拖延循环",
        "source_content": "专家确认",
        "response_template": "尝试设定一个时间段专注于完成一部分内容，即使不完美也没关系，重点是完成。您觉得可以试试吗？"
      },
      {
        "node_id": "lack_of_interest",
        "node_type": "decision",
        "label": "对复习缺乏兴趣，看不到学习的意义，难以自律",
        "status": "confirmed",
        "node_content": "动机是驱动行为的关键因素",
        "source_content": "专家确认",
        "prompt_template": "您觉得复习这些内容对您个人来说意味着什么？您对未来的目标有什么想法？",
        "children": [
          {
            "relationship": "solid",
            "edge_label": "寻找动机",
            "node_id": "find_motivation"
          }
        ]
      },
      {
        "node_id": "find_motivation",
        "node_type": "leaf",
        "label": "探索学习内容与个人长远目标、兴趣的联系，寻找内在动机，建立奖励机制，寻找学习伙伴",
        "status": "confirmed",
        "node_content": "提升学习动力，促进主动学习行为",
        "source_content": "专家确认",
        "response_template": "想想看，完成这次考试对实现您的某个长期目标有什么帮助？或者，有没有让学习过程更有趣一点的方法？"
      }
    ],
    "metadata": {
      "version": "2.1.0",
      "generate_time": "2025-04-11T00:00:00+08:00",
      "design_scope": {
        "applicable_scenarios": ["在线咨询", "自评工具", "心理辅导初步评估", "教育咨询"],
        "excluded_scenarios": ["紧急危机干预", "精神疾病诊断"]
      },
      "author": "clinical_design_team_adapted"
    }
  },
  // 添加测试决策树
  homework: {
    "tree_id": "homework",
    "tree_name": "测试决策树",
    "root": {
      "node_id": "root",
      "node_type": "decision",
      "label": "测试节点"
    },
    "node_registry": [
      {
        "node_id": "root",
        "node_type": "decision",
        "label": "测试节点",
        "node_content": "这是一个测试节点",
        "children": [
          {
            "relationship": "solid",
            "node_id": "child1"
          },
          {
            "relationship": "dashed",
            "node_id": "child2"
          }
        ]
      },
      {
        "node_id": "child1",
        "node_type": "leaf",
        "label": "子节点1",
        "node_content": "这是子节点1的内容",
        "response_template": "这是测试子节点1的响应"
      },
      {
        "node_id": "child2",
        "node_type": "leaf",
        "label": "子节点2",
        "node_content": "这是子节点2的内容",
        "response_template": "这是测试子节点2的响应"
      }
    ],
    "metadata": {
      "version": "1.0.0",
      "generate_time": "2025-04-11T00:00:00+08:00"
    }
  }
};

// 合并所有树
const allTrees = { ...staticTrees };

// 使用Pinia定义状态
export const useDecisionTreeStore = defineStore('decisionTree', {
  state: () => ({
    currentTreeId: null,
    currentTree: null,
    backendTrees: [], // 存储从后端获取的决策树
    modifications: {
      veto_decision_tree: 0,
      approve_decision_tree: 0,
      modify_tree_node: 0,
      remove_tree_node: 0,
      add_tree_node: 0
    },
    modifiedNodes: {}
  }),

  actions: {
    // 设置当前树ID
    setCurrentTree(treeId) {
      this.currentTreeId = treeId;
    },

    // 获取所有后端决策树
    async fetchAllBackendTrees() {
      try {
        console.log('开始获取后端所有决策树...');
        const treesData = await decisionTreeApi.getDecisionTrees();
        
        // 调试日志：记录API返回的原始数据
        console.log('API返回的原始数据:', {
          type: typeof treesData,
          isNull: treesData === null,
          isUndefined: treesData === undefined
        });
        
        // 确保treesData是有效数据
        if (!treesData) {
          console.warn('获取决策树返回了空数据', treesData);
          this.backendTrees = [];
          return [];
        }
        
        // 检查API返回的数据格式并处理
        if (Array.isArray(treesData)) {
          // 如果是数组，则直接使用
          console.log('成功获取后端决策树数组:', treesData.length);
          console.log('数组内容示例:', treesData.length > 0 ? JSON.stringify(treesData[0]).substring(0, 200) + '...' : '空数组');
          this.backendTrees = treesData;
          return treesData;
        }
        else if (treesData && typeof treesData === 'object') {
          // 对象详细信息日志
          console.log('API返回了对象类型数据:', Object.keys(treesData));
          
          // 检查是否有data字段（新API格式）
          if (treesData.data && Array.isArray(treesData.data)) {
            console.log('检测到新API格式，从data字段获取决策树数组');
            console.log('决策树数量:', treesData.data.length);
            
            // 处理新的格式转换为适合系统的格式
            const processedTrees = treesData.data.map((item, index) => {
              // 如果已有标准格式则直接使用
              if (item.tree_id && item.node_registry) {
                return item;
              }
              
              // 使用索引作为ID的一部分，确保ID唯一且可预测
              const treeId = `backend_tree_${index}`;
              const treeName = item.metadata?.author || '后端决策树';
              
              // 从树结构提取节点
              const nodeRegistry = [];
              const extractNodes = (node, parentId = null) => {
                if (!node) return null;
                
                // 创建唯一节点ID，使用父节点ID和节点标签组合
                const nodeId = parentId ? 
                  `${parentId}_${node.judge || node.state || 'node'}`.replace(/\s+/g, '_') : 
                  `root_${node.judge || node.state || 'node'}`.replace(/\s+/g, '_');
                
                // 检查节点类型
                const isLeaf = !node.has_sub_content;
                const nodeType = isLeaf ? 'leaf' : 'decision';
                
                // 创建节点对象
                const nodeObj = {
                  node_id: nodeId,
                  node_type: nodeType,
                  label: node.judge || node.state || '决策节点',
                  status: 'confirmed',
                  node_content: node.description || node.all || '',
                  children: []
                };
                
                // 添加特定字段
                if (isLeaf) {
                  nodeObj.response_template = node.homework || node.methods || '';
                } else {
                  nodeObj.prompt_template = node.question || '';
                }
                
                // 添加节点到注册表
                nodeRegistry.push(nodeObj);
                
                // 递归处理子节点
                if (node.sub_content && typeof node.sub_content === 'object') {
                  const childrenEntries = Object.entries(node.sub_content);
                  childrenEntries.forEach(([childLabel, childNode], index) => {
                    const childId = extractNodes(childNode, nodeId);
                    if (childId) {
                      nodeObj.children.push({
                        node_id: childId,
                        edge_label: childLabel,
                        relationship: 'solid'
                      });
                    }
                  });
                }
                
                return nodeId;
              };
              
              // 从树的根节点开始处理
              const rootNodeId = extractNodes(item.tree);
              
              // 创建标准格式的树对象
              return {
                tree_id: treeId,
                tree_name: treeName,
                root: {
                  node_id: rootNodeId,
                  node_type: 'decision',
                  label: item.tree?.judge || '根节点'
                },
                node_registry: nodeRegistry,
                metadata: item.metadata || {
                  version: '1.0.0',
                  generate_time: new Date().toISOString()
                },
                original_data: item // 保留原始数据以备后用
              };
            });
            
            this.backendTrees = processedTrees;
            return processedTrees;
          }
          
          // 以下是原有的处理逻辑
          if (treesData.tree_id) {
            // 单个决策树对象
            const treeArray = [treesData];
            console.log('成功获取后端单个决策树，转换为数组:', treesData.tree_id);
            this.backendTrees = treeArray;
            return treeArray;
          } 
          else if (Object.keys(treesData).length > 0) {
            // 可能是包含多个决策树的对象，其中键可能是tree_id
            const treeArray = Object.values(treesData).filter(item => item && typeof item === 'object');
            console.log('获取到决策树对象集合，转换为数组:', {
              原始键: Object.keys(treesData),
              有效对象数: treeArray.length,
              对象示例: treeArray.length > 0 ? JSON.stringify(treeArray[0]).substring(0, 200) + '...' : '无有效对象'
            });
            this.backendTrees = treeArray;
            return treeArray;
          }
        }
        
        // 如果格式不符合预期，添加详细日志
        console.warn('API返回的数据格式不符合预期:', {
          类型: typeof treesData,
          内容预览: typeof treesData === 'object' ? 
            JSON.stringify(treesData).substring(0, 200) + '...' : 
            String(treesData).substring(0, 200) + '...'
        });
        this.backendTrees = [];
        return [];
      } catch (error) {
        console.error('获取后端决策树失败:', error);
        console.error('错误详情:', {
          message: error.message,
          stack: error.stack
        });
        this.backendTrees = [];
        return [];
      }
    },

    // 获取所有可用的决策树(合并静态和后端)
    getAllTrees() {
      // 创建一个对象来存储合并后的树
      const mergedTrees = { ...staticTrees };
      
      // 将后端树添加到合并对象中
      this.backendTrees.forEach(tree => {
        if (tree && tree.tree_id) {
          mergedTrees[tree.tree_id] = tree;
        }
      });
      
      return mergedTrees;
    },

    // 从后端获取树数据
    async getTreeById(treeId) {
      try {
        console.log('从API获取决策树:', treeId);
        
        // 检查是否是后端决策树ID
        if (treeId.startsWith('backend_tree_')) {
          // 尝试从已缓存的后端树中查找
          const backendTree = this.backendTrees.find(tree => tree.tree_id === treeId);
          if (backendTree) {
            console.log('使用已缓存的后端树:', treeId);
            this.currentTree = backendTree;
            return backendTree;
          }
        }
        
        // 如果不是后端决策树或未找到，尝试从API获取
        const treeData = await decisionTreeApi.getDecisionTreeById(treeId);
        if (treeData) {
          this.currentTree = treeData;
          return treeData;
        }
        
        // 尝试从已缓存的后端树中查找
        const backendTree = this.backendTrees.find(tree => tree.tree_id === treeId);
        if (backendTree) {
          console.log('使用已缓存的后端树:', treeId);
          this.currentTree = backendTree;
          return backendTree;
        }
        
        // 尝试从静态数据中查找
        if (staticTrees[treeId]) {
          console.log('使用本地静态树:', treeId);
          this.currentTree = staticTrees[treeId];
          return staticTrees[treeId];
        }
        
        // 如果都没有找到，返回null
        console.error('无法获取决策树数据:', treeId);
        return null;
      } catch (error) {
        console.error('从API获取决策树失败:', error);
        
        // 尝试从已缓存的后端树中查找
        const backendTree = this.backendTrees.find(tree => tree.tree_id === treeId);
        if (backendTree) {
          console.log('使用已缓存的后端树:', treeId);
          this.currentTree = backendTree;
          return backendTree;
        }
        
        // 尝试从静态数据中查找
        if (staticTrees[treeId]) {
          console.log('使用本地静态树:', treeId);
          this.currentTree = staticTrees[treeId];
          return staticTrees[treeId];
        }
        
        // 如果都没有找到，返回null
        console.error('无法获取决策树数据:', treeId);
        return null;
      }
    },

    // 更新节点
    updateNode(nodeId, nodeData) {
      if (!this.currentTree) return false;
      console.log('Store: 开始更新节点', nodeId, nodeData);

      // 在注册表中查找并更新节点
      const updateNodeInRegistry = () => {
        for (let i = 0; i < this.currentTree.node_registry.length; i++) {
          const node = this.currentTree.node_registry[i];
          if (node.node_id === nodeId) {
            console.log('Store: 找到匹配的节点', node);
            
            // 记录修改
            this.modifications.modify_tree_node++;
            this.modifiedNodes[nodeId] = {
              type: 'modify',
              data: nodeData
            };
            
            // 备份节点的子节点信息
            const children = node.children;
            
            // 更新节点数据，保留children信息
            const updatedNode = {
              ...node,
              node_id: nodeId,
              label: nodeData.label,
              node_type: nodeData.node_type,
              status: nodeData.status || node.status,
              node_content: nodeData.node_content,
              source_content: nodeData.source_content,
              inference_relationship: nodeData.inference_relationship
            };
            
            // 根据节点类型保留特定字段
            if (nodeData.node_type === 'decision') {
              updatedNode.prompt_template = nodeData.prompt_template;
            } else if (nodeData.node_type === 'leaf') {
              updatedNode.response_template = nodeData.response_template;
            }
            
            // 确保保留children字段
            updatedNode.children = children;
            
            // 更新节点
            this.currentTree.node_registry[i] = updatedNode;
            console.log('Store: 节点更新后', this.currentTree.node_registry[i]);
            
            return true;
          }
        }
        return false;
      };

      const result = updateNodeInRegistry();
      console.log('Store: 节点更新结果', result);
      return result;
    },

    // 删除节点
    deleteNode(nodeId) {
      if (!this.currentTree) return false;

      // 在树中查找并删除节点
      const deleteNodeFromTree = (nodes) => {
        for (let i = 0; i < nodes.length; i++) {
          if (nodes[i].node_id === nodeId) {
            // 记录删除操作
            this.modifications.remove_tree_node++;
            this.modifiedNodes[nodeId] = {
              type: 'delete',
              data: nodes[i]
            };
            
            // 删除节点
            nodes.splice(i, 1);
            return true;
          }
          if (nodes[i].children && nodes[i].children.length > 0) {
            if (deleteNodeFromTree(nodes[i].children)) return true;
          }
        }
        return false;
      };

      return deleteNodeFromTree(this.currentTree.node_registry);
    },

    // 添加节点
    addNode(newNode, parentId, edgeInfo) {
      if (!this.currentTree) return false;

      // 在树中查找父节点并添加新节点
      const addNodeToParent = (nodes) => {
        for (let node of nodes) {
          if (node.node_id === parentId) {
            // 确保children数组存在
            if (!node.children) {
              node.children = [];
            }
            
            // 记录添加操作
            this.modifications.add_tree_node++;
            this.modifiedNodes[newNode.node_id] = {
              type: 'add',
              data: newNode
            };
            
            // 添加新节点到父节点的children中
            node.children.push({
              node_id: newNode.node_id,
              edge_label: edgeInfo.edge_label,
              relationship: edgeInfo.relationship
            });
            
            // 添加新节点到节点注册表
            this.currentTree.node_registry.push(newNode);
            return true;
          }
          if (node.children && node.children.length > 0) {
            if (addNodeToParent(node.children)) return true;
          }
        }
        return false;
      };

      return addNodeToParent(this.currentTree.node_registry);
    },

    // 更新节点状态
    updateNodeStatus(nodeId, status) {
      if (!this.currentTree) return false;

      const updateStatus = (nodes) => {
        for (let node of nodes) {
          if (node.node_id === nodeId) {
            // 记录状态变更
            if (status === 'reject') {
              this.modifications.veto_decision_tree++;
            } else if (status === 'confirm') {
              this.modifications.approve_decision_tree++;
            }
            
            node.status = status;
            this.modifiedNodes[nodeId] = {
              type: 'status_change',
              data: { status }
            };
            return true;
          }
          if (node.children && node.children.length > 0) {
            if (updateStatus(node.children)) return true;
          }
        }
        return false;
      };

      return updateStatus(this.currentTree.node_registry);
    },

    // 将前端树形结构转换为后端需要的嵌套对象结构
    convertToBackendFormat(node, parentLabel = null) {
      if (!node) return null;
      
      // 创建基础节点对象
      const nodeObj = {
        sure: node.status === 'confirmed' ? '已确认' : '确认',
        source: '专家确认',
        judge: node.label || '未命名节点',
        state: '未处理',
        question: node.prompt_template || null,
        has_sub_content: node.node_type === 'decision',
        description: node.node_content || null,
        all: node.inference_relationship || null,
        methods: null,
        homework: node.response_template || null,
        sub_content: {}
      };

      // 如果有子节点，递归处理
      if (node.children && node.children.length > 0) {
        node.children.forEach(child => {
          const childNode = this.currentTree.node_registry.find(n => n.node_id === child.node_id);
          if (childNode) {
            nodeObj.sub_content[child.edge_label] = this.convertToBackendFormat(childNode, child.edge_label);
          }
        });
      }

      return nodeObj;
    },

    // 准备后端数据
    prepareDataForBackend() {
      // 确保当前树存在
      if (!this.currentTree) {
        console.error('当前树不存在');
        return null;
      }

      // 准备专家决策树统计表
      const expertDecisionTreeExport = Object.entries(this.modifications)
        .filter(([_, count]) => count > 0)
        .map(([type, count]) => ({
          type,
          count
        }));

      // 获取根节点
      const rootNode = this.currentTree.node_registry.find(node => node.node_id === this.currentTree.root.node_id);
      if (!rootNode) {
        console.error('找不到根节点');
        return null;
      }

      // 转换为后端格式
      const backendTree = this.convertToBackendFormat(rootNode);

      // 构建完整的请求数据
      const requestData = {
        tree: backendTree,
        tree_id: this.currentTree.tree_id,
        tree_name: this.currentTree.tree_name,
        metadata: this.currentTree.metadata || {
          version: '1.0.0',
          generate_time: new Date().toISOString(),
          design_scope: {
            applicable_scenarios: ['在线咨询', '自评工具', '心理辅导初步评估', '教育咨询'],
            excluded_scenarios: ['紧急危机干预', '精神疾病诊断']
          },
          author: 'clinical_design_team_adapted'
        }
      };

      // 打印请求数据，方便调试
      console.log('准备发送到后端的数据:', requestData);

      return requestData;
    },

    // 保存修改到后端
    async saveModifications() {
      if (!this.currentTreeId || !this.currentTree) return;

      try {
        const requestData = this.prepareDataForBackend();
        console.log('原始请求数据:', requestData);
        console.log('原始请求数据类型:', typeof requestData);
        
        // 将请求数据转换为字符串
        const requestDataString = JSON.stringify(requestData);
        console.log('转换后的字符串数据:', requestDataString);
        console.log('转换后的数据类型:', typeof requestDataString);
        
        // 尝试解析回对象，验证格式是否正确
        const parsedData = JSON.parse(requestDataString);
        console.log('解析回对象后的数据:', parsedData);
        console.log('解析后的数据类型:', typeof parsedData);
        
        await decisionTreeApi.updateDecisionTree(this.currentTreeId, requestDataString);
        
        // 清除修改记录
        this.clearModifications();
        return true;
      } catch (error) {
        console.error('保存修改失败:', error);
        throw error;
      }
    },

    // 清除修改记录
    clearModifications() {
      this.modifications = {
        veto_decision_tree: 0,
        approve_decision_tree: 0,
        modify_tree_node: 0,
        remove_tree_node: 0,
        add_tree_node: 0
      };
      this.modifiedNodes = {};
    },

    // 获取特定决策树
    async getDecisionTreeById(treeId) {
      try {
        console.log(`请求决策树 ${treeId}...`);
        
        // 检查是否是模拟数据ID
        if (treeId === 'from_backend_example') {
          console.log('返回模拟决策树数据');
          return mockDecisionTree;
        }
        
        // 检查是否是静态树ID
        if (treeId === 'anxiety_management_v1' || treeId === 'stress' || treeId === 'homework') {
          console.log('使用静态决策树数据');
          return allTrees[treeId];
        }
        
        try {
          // 1. 首先通过axios获取
          const response = await apiClient.get(`/decision_trees/${treeId}`);
          console.log(`通过axios获取决策树 ${treeId} 成功:`, response.data);
          return response.data;
        } catch (axiosError) {
          console.warn(`通过axios获取决策树 ${treeId} 失败:`, axiosError);
          
          try {
            // 2. 尝试通过fetch获取
            const response = await fetch(`/api/decision_trees/${treeId}`, {
              method: 'GET',
              headers: {
                'Accept': 'application/json'
              },
              mode: 'cors'
            });
            
            if (!response.ok) {
              throw new Error(`状态码: ${response.status}`);
            }
            
            const data = await response.json();
            return data;
          } catch (fetchError) {
            console.warn(`通过fetch获取决策树 ${treeId} 失败:`, fetchError);
            
            // 3. 尝试从已缓存的后端树中查找
            const backendTree = this.backendTrees?.find(tree => tree.tree_id === treeId);
            if (backendTree) {
              console.log('使用已缓存的后端树:', treeId);
              return backendTree;
            }
            
            // 4. 尝试从静态数据中查找
            if (allTrees[treeId]) {
              console.log('使用静态决策树数据:', treeId);
              return allTrees[treeId];
            }
            
            // 5. 如果都找不到，返回模拟数据
            console.warn(`无法找到决策树 ${treeId}，返回模拟数据`);
            return mockDecisionTree;
          }
        }
      } catch (error) {
        console.error(`获取决策树 ${treeId} 失败:`, error);
        
        // 尝试从静态数据中查找
        if (allTrees[treeId]) {
          console.log('使用静态决策树数据:', treeId);
          return allTrees[treeId];
        }
        
        // 如果都找不到，返回模拟数据
        console.warn(`无法找到决策树 ${treeId}，返回模拟数据`);
        return mockDecisionTree;
      }
    }
  }
});

// 递归转换后端嵌套tree为前端树形结构
export function convertBackendTree(node, branchLabel = null) {
  if (!node) return null;
  const result = {
    label: node.judge || node.state || '未命名节点',
    description: node.description || '',
    question: node.question || '',
    state: node.state || '',
    branch: branchLabel, // 分支名
    children: []
  };
  if (node.sub_content && typeof node.sub_content === 'object') {
    for (const [branch, childNode] of Object.entries(node.sub_content)) {
      const child = convertBackendTree(childNode, branch);
      if (child) {
        result.children.push(child);
      }
    }
  }
  return result;
} 