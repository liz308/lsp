#include <gtest/gtest.h>
#include "ParameterQueue.h"
#include <thread>
#include <vector>

/**
 * Unit tests for ParameterQueue lock-free SPSC queue.
 * Tests basic functionality, thread safety, and edge cases.
 */

class ParameterQueueTest : public ::testing::Test {
protected:
    ParameterQueue queue;
};

TEST_F(ParameterQueueTest, EnqueueDequeueBasic) {
    ParameterUpdate update;
    
    // Queue should be empty initially
    EXPECT_TRUE(queue.empty());
    EXPECT_FALSE(queue.dequeue(&update));
    
    // Enqueue a parameter update
    EXPECT_TRUE(queue.enqueue(0, 12.5f));
    EXPECT_FALSE(queue.empty());
    
    // Dequeue the parameter update
    EXPECT_TRUE(queue.dequeue(&update));
    EXPECT_EQ(0, update.port_index);
    EXPECT_FLOAT_EQ(12.5f, update.value);
    
    // Queue should be empty again
    EXPECT_TRUE(queue.empty());
}

TEST_F(ParameterQueueTest, MultipleEnqueueDequeue) {
    ParameterUpdate update;
    
    // Enqueue multiple updates
    EXPECT_TRUE(queue.enqueue(0, 1.0f));
    EXPECT_TRUE(queue.enqueue(1, 2.0f));
    EXPECT_TRUE(queue.enqueue(2, 3.0f));
    
    // Dequeue in FIFO order
    EXPECT_TRUE(queue.dequeue(&update));
    EXPECT_EQ(0, update.port_index);
    EXPECT_FLOAT_EQ(1.0f, update.value);
    
    EXPECT_TRUE(queue.dequeue(&update));
    EXPECT_EQ(1, update.port_index);
    EXPECT_FLOAT_EQ(2.0f, update.value);
    
    EXPECT_TRUE(queue.dequeue(&update));
    EXPECT_EQ(2, update.port_index);
    EXPECT_FLOAT_EQ(3.0f, update.value);
    
    EXPECT_TRUE(queue.empty());
}

TEST_F(ParameterQueueTest, QueueFull) {
    // Fill the queue to capacity
    for (int i = 0; i < ParameterQueue::QUEUE_SIZE - 1; ++i) {
        EXPECT_TRUE(queue.enqueue(i % 10, static_cast<float>(i)));
    }
    
    // Queue should be full now
    EXPECT_TRUE(queue.full());
    
    // Next enqueue should fail
    EXPECT_FALSE(queue.enqueue(99, 99.0f));
    
    // Dequeue one item
    ParameterUpdate update;
    EXPECT_TRUE(queue.dequeue(&update));
    
    // Now we should be able to enqueue again
    EXPECT_FALSE(queue.full());
    EXPECT_TRUE(queue.enqueue(99, 99.0f));
}

TEST_F(ParameterQueueTest, NullPointerHandling) {
    // Enqueue should work
    EXPECT_TRUE(queue.enqueue(0, 1.0f));
    
    // Dequeue with null pointer should fail
    EXPECT_FALSE(queue.dequeue(nullptr));
    
    // Queue should still have the item
    EXPECT_FALSE(queue.empty());
}

TEST_F(ParameterQueueTest, SizeTracking) {
    EXPECT_EQ(0, queue.size());
    
    queue.enqueue(0, 1.0f);
    EXPECT_EQ(1, queue.size());
    
    queue.enqueue(1, 2.0f);
    EXPECT_EQ(2, queue.size());
    
    ParameterUpdate update;
    queue.dequeue(&update);
    EXPECT_EQ(1, queue.size());
    
    queue.dequeue(&update);
    EXPECT_EQ(0, queue.size());
}

TEST_F(ParameterQueueTest, WrapAround) {
    ParameterUpdate update;
    
    // Fill and drain multiple times to test wrap-around
    for (int cycle = 0; cycle < 3; ++cycle) {
        for (int i = 0; i < 10; ++i) {
            EXPECT_TRUE(queue.enqueue(i, static_cast<float>(i + cycle * 10)));
        }
        
        for (int i = 0; i < 10; ++i) {
            EXPECT_TRUE(queue.dequeue(&update));
            EXPECT_EQ(i, update.port_index);
            EXPECT_FLOAT_EQ(static_cast<float>(i + cycle * 10), update.value);
        }
        
        EXPECT_TRUE(queue.empty());
    }
}

TEST_F(ParameterQueueTest, ThreadSafety) {
    // Producer thread: enqueue updates
    std::vector<std::thread> threads;
    
    // Start producer thread
    threads.emplace_back([this]() {
        for (int i = 0; i < 100; ++i) {
            while (!queue.enqueue(i % 10, static_cast<float>(i))) {
                // Retry if queue is full
                std::this_thread::yield();
            }
        }
    });
    
    // Consumer thread: dequeue updates
    int dequeued_count = 0;
    threads.emplace_back([this, &dequeued_count]() {
        ParameterUpdate update;
        while (dequeued_count < 100) {
            if (queue.dequeue(&update)) {
                dequeued_count++;
            } else {
                std::this_thread::yield();
            }
        }
    });
    
    // Wait for both threads to complete
    for (auto& t : threads) {
        t.join();
    }
    
    EXPECT_EQ(100, dequeued_count);
    EXPECT_TRUE(queue.empty());
}

TEST_F(ParameterQueueTest, ParameterValuePrecision) {
    ParameterUpdate update;
    
    // Test various floating-point values
    float test_values[] = {
        0.0f, 1.0f, -1.0f, 0.5f, -0.5f,
        1000.0f, -1000.0f, 0.001f, -0.001f,
        3.14159f, -3.14159f
    };
    
    for (float value : test_values) {
        EXPECT_TRUE(queue.enqueue(0, value));
        EXPECT_TRUE(queue.dequeue(&update));
        EXPECT_FLOAT_EQ(value, update.value);
    }
}
