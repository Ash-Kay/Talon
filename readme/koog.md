Koog: Kotlin AI Agent Framework

Official JetBrains Framework for KMM AI Agents
Overview

Koog is an open-source, JetBrains-developed framework for building predictable, fault-tolerant AI
agents with an idiomatic, type-safe Kotlin DSL. It is designed for JVM, JS, WasmJS, Android, and iOS
targets via Kotlin Multiplatform (KMM).

Unlike standard LLM wrappers, Koog provides out-of-the-box state machine persistence, intelligent
history compression, graph-based workflow strategies, and advanced goal-oriented planning.
Core Components

1. Agent Types

Koog supports three primary architectures depending on the complexity of the task:

    Basic Agents (Single-Run): Designed for straightforward tasks. The agent processes a single input, executes a single cycle of tool-calling, and returns a final response.

    Complex Workflow Agents (Graph-Based): Uses a Strategy graph to guide the agent. These agents can loop, fallback on errors, and pause execution (e.g., waiting for an Android UI to settle), retaining their exact state in the execution graph.

    Planner Agents: Separates the creation of a plan from its execution. Supports Simple LLM planners and advanced GOAP (Goal-Oriented Action Planning) architectures.

2. Prompt Executors (LLM Clients)

Executors are wrappers that handle the actual HTTP calls to the LLM providers. Koog supports
seamless model switching without losing conversation history.

    Supported Providers: OpenAI, Anthropic, Google (Gemini), DeepSeek, Bedrock, OpenRouter, and local models via Ollama.

    Implementation: Instantiated via helper functions like simpleOpenAIExecutor(apiKey) or resilient setups using RetryingLLMClient.

3. Tools (ToolRegistry)

Tools are the actions an agent can take to interact with the outside world (e.g., clicking an
Android button).

    Class-Based (Required for KMM): Extend the Tool<Args, Result>() abstract class. Define an Args data class annotated with @Serializable and @LLMDescription, and override the execute(args) method.

    MCP Support: Koog natively supports the Model Context Protocol (MCP) to consume external tools directly.

4. Memory & State Persistence

   Agent Persistence: Koog saves the entire state machine natively. You can configure it to
   automatically create checkpoints after every node execution.

   History Compression: Built-in token optimization techniques to summarize or truncate long-running
   conversation loops automatically to save API costs.

Code Examples (For Editor Context)

1. Defining a Class-Based Tool (KMM Compatible)
   Kotlin

import ai.koog.agent.Tool
import kotlinx.serialization.Serializable

class ClickElementTool : Tool<ClickElementTool.Args, Boolean>() {
override val description = "Clicks a UI element on the screen based on its text"

    @Serializable
    data class Args(
        @property:LLMDescription("The exact text of the button to click")
        val targetText: String
    )

    override suspend fun execute(args: Args): Boolean {
        // Platform-specific execution logic here
        return true
    }

}

2. Creating a Graph-Based Strategy
   Kotlin

import ai.koog.agent.strategy.strategy

val customStrategy = strategy<String, String>("MobileUIWorkflow") {
val nodeCallLLM by nodeLLMRequest()
val nodeExecuteTool by nodeExecuteTool()
val nodeSendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo nodeCallLLM)
    edge(nodeCallLLM forwardTo nodeExecuteTool onCondition { it.hasToolCalls() })
    edge(nodeExecuteTool forwardTo nodeSendToolResult)
    edge(nodeSendToolResult forwardTo nodeCallLLM) // Loop back to LLM after tool result
    edge(nodeCallLLM forwardTo nodeFinish onCondition { !it.hasToolCalls() })

}

3. Planner Agents: GOAP (Goal-Oriented Action Planning)

For agents that need to plan multiple steps ahead, Koog provides a GOAP planner. It uses A* search
combined with LLM reasoning to find the best sequence of actions to reach a specific state.
Kotlin

import ai.koog.agent.AIAgentConfig
import ai.koog.agent.PlannerAIAgent
import ai.koog.agent.planner.goap
import ai.koog.agent.strategy.AIAgentPlannerStrategy

// 1. Define the State as a data class
data class AppTaskState(
val appOpened: Boolean = false,
val noteCreated: Boolean = false,
val taskComplete: Boolean = false
)

// 2. Define the GOAP Planner
val myPlanner = goap<AppTaskState> {

    action(
        name = "Open App",
        precondition = { !it.appOpened },
        belief = { it.copy(appOpened = true) }
    ) { state ->
        // Native launch app logic
        state.copy(appOpened = true)
    }

    action(
        name = "Create Note",
        precondition = { it.appOpened && !it.noteCreated },
        belief = { it.copy(noteCreated = true) }
    ) { state ->
        // Native click logic
        state.copy(noteCreated = true)
    }

    // Define the ultimate goal
    goal(
        name = "Complete Task",
        condition = { it.noteCreated }
    )

}

// 3. Wrap in Strategy and initialize Agent
val plannerStrategy = AIAgentPlannerStrategy("task-planner", myPlanner)

val agent = PlannerAIAgent(
promptExecutor = simpleOpenAIExecutor("YOUR_API_KEY"),
strategy = plannerStrategy
)

4. Simple LLM Planners

If you don't need strict typed states, Koog also provides SimpleLLMPlanner and
SimpleLLMWithCriticPlanner which operate on string-based states and dynamically assess if they need
to replan mid-execution.
Kotlin

import ai.koog.agent.planner.SimpleLLMPlanner

val simplePlanner = SimpleLLMPlanner()
val strategy = AIAgentPlannerStrategy("simple-planner", simplePlanner)

val agent = PlannerAIAgent(
promptExecutor = simpleOpenAIExecutor("YOUR_API_KEY"),
strategy = strategy
)