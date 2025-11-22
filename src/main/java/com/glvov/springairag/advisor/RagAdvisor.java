package com.glvov.springairag.advisor;

import com.glvov.springairag.advisor.misc.BM25RerankEngine;
import com.glvov.springairag.utils.FileLoader;
import lombok.Builder;
import lombok.Getter;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.glvov.springairag.advisor.ExpansionQueryAdvisor.ENRICHED_QUESTION;
import static com.glvov.springairag.utils.FileLoader.loadFile;

@Builder
public class RagAdvisor implements BaseAdvisor {

    private static final String RAG_PROMPT = loadFile("/ai/prompts/rag.txt");

    /**
     * <p>
     * <b>Filtering by quantity (topK):</b>
     * <br>
     * The {@code topK} parameter specifies the maximum number of <b>chunks</b> (not tokens like for LLM request)
     * to retrieve from RAG for each query.
     * For example, {@code topK(4)} means only the 4 most relevant chunks will be considered.
     * </p>
     * <p>
     * <b>Filtering by quality (similarityThreshold):</b>
     * <br>
     * The {@code similarityThreshold} parameter defines the minimum similarity score
     * required for a document to be considered relevant. Documents with lower similarity
     * values are discarded.
     * <ul>
     *   <li><code>0.0</code> → all documents are accepted, even the least relevant ones</li>
     *   <li><code>0.8–1.0</code> → very strict threshold — only documents almost identical to the query</li>
     * </ul>
     * </p>
     */
    @Builder.Default
    private SearchRequest searchRequest = SearchRequest
            .builder()
            .topK(4)
            .similarityThreshold(0.62)
            .build();

    @Builder.Default
    private final boolean rerankEnabled = true;

    @Getter
    private final int order;

    private final VectorStore vectorStore;


    public static RagAdvisorBuilder build(VectorStore vectorStore) {
        return new RagAdvisorBuilder().vectorStore(vectorStore);
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        String originalUserQuestion = chatClientRequest.prompt().getUserMessage().getText();

        String queryToRag = chatClientRequest.context()
                .getOrDefault(ENRICHED_QUESTION, originalUserQuestion)
                .toString();

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest
                        .from(searchRequest)
                        .query(queryToRag)
                        .topK(rerankEnabled ? searchRequest.getTopK() * 2 : searchRequest.getTopK())
                        .build()
        );

        if (documents.isEmpty()) {
            return chatClientRequest
                    .mutate()
                    .context("CONTEXT", "No documents found in Vector Store")
                    .build();
        }

        if (rerankEnabled) {
            BM25RerankEngine rerankEngine = BM25RerankEngine.builder().build();
            documents = rerankEngine.rerank(documents, queryToRag, searchRequest.getTopK());
        }

        // this is not the context like in ExpansionAdvisor, it is the context for the LLM inside the prompt
        String promptContext = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining(System.lineSeparator()));

        String finalUserPrompt = new PromptTemplate(RAG_PROMPT)
                .render(Map.of("context", promptContext, "question", originalUserQuestion));

        return chatClientRequest
                .mutate()
                .prompt(chatClientRequest.prompt().augmentUserMessage(finalUserPrompt))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }
}