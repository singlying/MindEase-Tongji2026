<template>
  <div class="live2d-card" :class="[`emotion-${emotion}`, { speaking, thinking }]">
    <div class="live2d-stage" ref="stageRef">
      <canvas ref="canvasRef" class="live2d-canvas"></canvas>
      <div class="stage-glow glow-left"></div>
      <div class="stage-glow glow-right"></div>
      <div class="stage-grid"></div>
    </div>
    <div class="live2d-copy">
      <span class="copy-label">Live2D Companion</span>
      <p class="copy-title">{{ title }}</p>
      <p class="copy-text">{{ description }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import * as PIXI from "pixi.js";
import { Live2DModel, MotionPriority } from "pixi-live2d-display/cubism4";

type AgentEmotion =
  | "steady"
  | "listening"
  | "soothing"
  | "encouraging"
  | "warm"
  | "celebrating"
  | "alert";

type MotionCue =
  | "neutral"
  | "concern"
  | "encouragement"
  | "surprise"
  | "shy";

const props = defineProps<{
  emotion: AgentEmotion;
  speaking: boolean;
  thinking: boolean;
  title: string;
  description: string;
  motionCue: MotionCue;
  motionSeed: number;
}>();

type Live2DModelInstance = InstanceType<typeof Live2DModel>;

const stageRef = ref<HTMLDivElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);
const model = ref<Live2DModelInstance | null>(null);
const app = ref<PIXI.Application | null>(null);
const resizeObserver = ref<ResizeObserver | null>(null);
let expressionPhase = 0;

const focusTargets: Record<AgentEmotion, { x: number; y: number }> = {
  steady: { x: 0, y: 0.05 },
  listening: { x: -0.18, y: 0.12 },
  soothing: { x: 0, y: 0.16 },
  encouraging: { x: 0.15, y: -0.02 },
  warm: { x: -0.08, y: 0.02 },
  celebrating: { x: 0.12, y: -0.08 },
  alert: { x: 0, y: -0.04 },
};

type CoreModelLike = {
  setParameterValueById: (id: string, value: number, weight?: number) => void;
};

type MotionManagerLike = {
  stopAllMotions: () => void;
  groups?: {
    idle?: string;
  };
};

const motionPlan: Record<MotionCue, { group: string; index: number; priority?: MotionPriority; durationMs: number }> = {
  neutral: { group: "Idle", index: 2, priority: MotionPriority.NORMAL, durationMs: 4300 },
  concern: { group: "Idle", index: 8, priority: MotionPriority.NORMAL, durationMs: 4300 },
  encouragement: { group: "Idle", index: 6, priority: MotionPriority.NORMAL, durationMs: 2200 },
  surprise: { group: "Idle", index: 5, priority: MotionPriority.NORMAL, durationMs: 2000 },
  shy: { group: "TapBody", index: 0, priority: MotionPriority.FORCE, durationMs: 4500 },
};

type ExpressionPreset = {
  angleX: number;
  angleY: number;
  angleZ: number;
  bodyAngleX: number;
  bodyAngleY: number;
  eyeBallX: number;
  eyeBallY: number;
  eyeOpen: number;
  eyeSmile: number;
  browY: number;
  browAngle: number;
  browForm: number;
  mouthForm: number;
  mouthOpen: number;
  cheek: number;
};

const expressionPresets: Record<AgentEmotion, ExpressionPreset> = {
  steady: {
    angleX: 0,
    angleY: -2,
    angleZ: 0,
    bodyAngleX: 0,
    bodyAngleY: 0,
    eyeBallX: 0,
    eyeBallY: 0.05,
    eyeOpen: 1,
    eyeSmile: 0.08,
    browY: 0.2,
    browAngle: 0.08,
    browForm: 0.1,
    mouthForm: 0.18,
    mouthOpen: 0.02,
    cheek: 0.08,
  },
  listening: {
    angleX: -3,
    angleY: -1,
    angleZ: -1.5,
    bodyAngleX: -2,
    bodyAngleY: 0,
    eyeBallX: -0.12,
    eyeBallY: 0.08,
    eyeOpen: 0.9,
    eyeSmile: 0.04,
    browY: 0.1,
    browAngle: -0.04,
    browForm: 0.18,
    mouthForm: 0.04,
    mouthOpen: 0.01,
    cheek: 0.04,
  },
  soothing: {
    angleX: -1,
    angleY: 1,
    angleZ: -2,
    bodyAngleX: -1,
    bodyAngleY: 0,
    eyeBallX: 0,
    eyeBallY: 0.12,
    eyeOpen: 0.76,
    eyeSmile: 0.46,
    browY: 0.28,
    browAngle: 0.14,
    browForm: 0.26,
    mouthForm: 0.48,
    mouthOpen: 0.04,
    cheek: 0.34,
  },
  encouraging: {
    angleX: 4,
    angleY: -1,
    angleZ: 1.5,
    bodyAngleX: 2,
    bodyAngleY: 0,
    eyeBallX: 0.1,
    eyeBallY: 0,
    eyeOpen: 1,
    eyeSmile: 0.18,
    browY: 0.34,
    browAngle: 0.12,
    browForm: 0.08,
    mouthForm: 0.58,
    mouthOpen: 0.05,
    cheek: 0.16,
  },
  warm: {
    angleX: 1,
    angleY: 0,
    angleZ: 0.6,
    bodyAngleX: 1,
    bodyAngleY: 0,
    eyeBallX: 0.03,
    eyeBallY: 0.06,
    eyeOpen: 0.9,
    eyeSmile: 0.28,
    browY: 0.22,
    browAngle: 0.1,
    browForm: 0.18,
    mouthForm: 0.42,
    mouthOpen: 0.03,
    cheek: 0.24,
  },
  celebrating: {
    angleX: 3,
    angleY: -3,
    angleZ: 2.5,
    bodyAngleX: 3,
    bodyAngleY: 0,
    eyeBallX: 0.06,
    eyeBallY: -0.04,
    eyeOpen: 0.84,
    eyeSmile: 0.78,
    browY: 0.4,
    browAngle: 0.2,
    browForm: -0.08,
    mouthForm: 0.82,
    mouthOpen: 0.08,
    cheek: 0.42,
  },
  alert: {
    angleX: 0,
    angleY: -4,
    angleZ: 0,
    bodyAngleX: 0,
    bodyAngleY: 0,
    eyeBallX: 0,
    eyeBallY: -0.08,
    eyeOpen: 1,
    eyeSmile: 0,
    browY: -0.04,
    browAngle: -0.32,
    browForm: 0.48,
    mouthForm: -0.42,
    mouthOpen: 0.02,
    cheek: 0,
  },
};

const modelScale = computed(() => {
  switch (props.emotion) {
    case "celebrating":
      return 0.165;
    case "encouraging":
      return 0.16;
    default:
      return 0.155;
  }
});

const applyModelLayout = () => {
  if (!stageRef.value || !app.value || !model.value) return;

  const width = stageRef.value.clientWidth;
  const height = stageRef.value.clientHeight;
  app.value.renderer.resize(width, height);

  model.value.anchor.set(0.5, 0);
  model.value.position.set(width * 0.5, height * 0.02);
  model.value.scale.set(modelScale.value);
};

const applyFocus = (instant = false) => {
  if (!model.value) return;
  const target = focusTargets[props.emotion];
  model.value.focus(target.x, target.y, instant);
};

const setMirroredParameter = (coreModel: CoreModelLike, leftId: string, rightId: string, value: number) => {
  coreModel.setParameterValueById(leftId, value, 1);
  coreModel.setParameterValueById(rightId, value, 1);
};

const getMotionManager = (): MotionManagerLike | undefined => {
  return model.value?.internalModel?.motionManager as MotionManagerLike | undefined;
};

const freezeMotionLoop = () => {
  const motionManager = getMotionManager();
  if (!motionManager) return;

  if (motionManager.groups) {
    motionManager.groups.idle = "__disabled_idle__";
  }
  motionManager.stopAllMotions();
};

const playMotionCue = async (cue: MotionCue) => {
  if (!model.value) return;

  const plan = motionPlan[cue];
  if (!plan) {
    freezeMotionLoop();
    return;
  }

  try {
    await model.value.motion(plan.group, plan.index, plan.priority ?? MotionPriority.NORMAL);
  } catch (_error) {
    freezeMotionLoop();
    return;
  }

  window.setTimeout(() => {
    freezeMotionLoop();
  }, plan.durationMs);
};

const applyExpressionParameters = (delta = 1) => {
  const coreModel = model.value?.internalModel?.coreModel as CoreModelLike | undefined;
  if (!coreModel) return;

  expressionPhase += Math.min(delta, 2) * 0.045;
  const preset = expressionPresets[props.emotion];
  const breath = Math.sin(expressionPhase);
  const micro = Math.sin(expressionPhase * 0.65);
  const speakingOpen = props.speaking ? 0.22 + Math.abs(Math.sin(expressionPhase * 3.6)) * 0.38 : 0;
  const thinkingSquint = props.thinking ? (Math.sin(expressionPhase * 2.2) > 0.72 ? 0.32 : 0) : 0;
  const mouthOpen = Math.min(1, preset.mouthOpen + speakingOpen);
  const eyeOpen = Math.max(0.4, preset.eyeOpen - thinkingSquint);

  coreModel.setParameterValueById("ParamAngleX", preset.angleX + micro * 1.4, 1);
  coreModel.setParameterValueById("ParamAngleY", preset.angleY + breath * 0.8, 1);
  coreModel.setParameterValueById("ParamAngleZ", preset.angleZ + breath * 0.6, 1);
  coreModel.setParameterValueById("ParamBodyAngleX", preset.bodyAngleX + micro * 1.1, 1);
  coreModel.setParameterValueById("ParamBodyAngleY", preset.bodyAngleY + breath * 0.4, 1);
  coreModel.setParameterValueById("ParamEyeBallX", preset.eyeBallX, 1);
  coreModel.setParameterValueById("ParamEyeBallY", preset.eyeBallY, 1);
  coreModel.setParameterValueById("ParamEyeLOpen", eyeOpen, 1);
  coreModel.setParameterValueById("ParamEyeROpen", eyeOpen, 1);
  coreModel.setParameterValueById("ParamEyeLSmile", preset.eyeSmile, 1);
  coreModel.setParameterValueById("ParamEyeRSmile", preset.eyeSmile, 1);
  setMirroredParameter(coreModel, "ParamBrowLY", "ParamBrowRY", preset.browY + breath * 0.04);
  setMirroredParameter(coreModel, "ParamBrowLAngle", "ParamBrowRAngle", preset.browAngle);
  setMirroredParameter(coreModel, "ParamBrowLForm", "ParamBrowRForm", preset.browForm);
  coreModel.setParameterValueById("ParamMouthForm", preset.mouthForm, 1);
  coreModel.setParameterValueById("ParamMouthOpenY", mouthOpen, 1);
  coreModel.setParameterValueById("ParamCheek", preset.cheek, 1);
};

const initializeLive2D = async () => {
  if (!stageRef.value || !canvasRef.value) return;

  (window as Window & { PIXI?: typeof PIXI }).PIXI = PIXI;

  const instance = new PIXI.Application({
    view: canvasRef.value,
    autoStart: true,
    resizeTo: stageRef.value,
    transparent: true,
    antialias: true,
    backgroundAlpha: 0,
  });
  app.value = instance;

  const loadedModel = await Live2DModel.from("/live2d/Hiyori/Hiyori.model3.json");
  model.value = loadedModel as Live2DModelInstance;
  loadedModel.interactive = true;
  loadedModel.buttonMode = true;

  loadedModel.on("hit", (hitAreas: string[]) => {
    if (hitAreas.includes("Body")) {
      void playMotionCue("shy");
    }
  });

  instance.stage.addChild(loadedModel);
  applyModelLayout();
  applyFocus(true);
  freezeMotionLoop();
  app.value?.ticker.add((deltaTime) => {
    applyExpressionParameters(deltaTime);
    freezeMotionLoop();
  });
  applyExpressionParameters();

  resizeObserver.value = new ResizeObserver(() => {
    applyModelLayout();
  });
  resizeObserver.value.observe(stageRef.value);
};

onMounted(async () => {
  await nextTick();
  await initializeLive2D();
});

watch(
  () => [props.emotion, props.motionCue, props.motionSeed],
  () => {
    applyModelLayout();
    applyFocus();
    applyExpressionParameters();
    void playMotionCue(props.motionCue);
  }
);

watch(
  () => props.speaking,
  (speakingNow) => {
    if (speakingNow) {
      applyFocus();
    }
    applyExpressionParameters();
  }
);

watch(
  () => props.thinking,
  (thinkingNow) => {
    if (thinkingNow) {
      applyFocus();
    }
    applyExpressionParameters();
    freezeMotionLoop();
  }
);

onBeforeUnmount(() => {
  resizeObserver.value?.disconnect();
  resizeObserver.value = null;

  if (model.value) {
    model.value.destroy();
    model.value = null;
  }

  if (app.value) {
    app.value.destroy(true, { children: true, texture: false, baseTexture: false });
    app.value = null;
  }
});
</script>

<style scoped>
.live2d-card {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 14px;
  min-width: 0;
  padding: 14px;
  min-height: 540px;
  border-radius: 26px;
  background: rgba(255, 255, 255, 0.76);
  border: 1px solid rgba(255, 255, 255, 0.85);
  box-shadow: 0 18px 38px rgba(31, 38, 135, 0.08);
}

.live2d-stage {
  position: relative;
  width: 100%;
  height: 440px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: 24px;
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(245, 251, 247, 0.96) 0%, rgba(230, 240, 234, 0.96) 100%);
}

.live2d-canvas {
  position: relative;
  z-index: 2;
  display: block;
  width: 100%;
  height: 100%;
}

.stage-glow,
.stage-grid {
  position: absolute;
  pointer-events: none;
}

.stage-glow {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  filter: blur(12px);
  opacity: 0.7;
}

.glow-left {
  top: 18px;
  left: 8px;
  background: rgba(251, 191, 36, 0.28);
}

.glow-right {
  top: 34px;
  right: 8px;
  background: rgba(167, 139, 250, 0.24);
}

.stage-grid {
  inset: auto 14px 16px 14px;
  height: 48px;
  border-radius: 20px;
  background:
    linear-gradient(rgba(123, 158, 137, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(123, 158, 137, 0.06) 1px, transparent 1px);
  background-size: 12px 12px;
  z-index: 1;
  mask-image: linear-gradient(180deg, transparent, rgba(0, 0, 0, 0.65));
}

.live2d-copy {
  min-width: 0;
  padding: 0 4px 2px;
  margin-top: auto;
}

.copy-label {
  display: inline-block;
  margin-bottom: 4px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--gray-400);
}

.copy-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: var(--ease-dark);
}

.copy-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: var(--gray-500);
}

.emotion-listening .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(242, 248, 255, 0.96) 0%, rgba(225, 236, 251, 0.96) 100%);
}

.emotion-soothing .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(247, 241, 255, 0.96) 0%, rgba(235, 228, 250, 0.96) 100%);
}

.emotion-encouraging .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(241, 252, 245, 0.96) 0%, rgba(226, 242, 232, 0.96) 100%);
}

.emotion-warm .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(255, 248, 236, 0.96) 0%, rgba(247, 235, 215, 0.96) 100%);
}

.emotion-celebrating .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(255, 242, 248, 0.96) 0%, rgba(246, 225, 239, 0.96) 100%);
}

.emotion-alert .live2d-stage {
  background:
    radial-gradient(circle at 50% 10%, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0) 34%),
    linear-gradient(180deg, rgba(255, 245, 243, 0.96) 0%, rgba(248, 228, 224, 0.96) 100%);
}

.speaking .live2d-stage,
.thinking .live2d-stage {
  box-shadow: inset 0 0 0 1px rgba(123, 158, 137, 0.12);
}

@media (max-width: 768px) {
  .live2d-card {
    width: 100%;
    min-height: 440px;
  }

  .live2d-stage {
    height: 340px;
  }
}
</style>
