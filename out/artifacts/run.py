import subprocess
import psutil

# 执行Java程序100次
for i in range(1, 5):
    print(f"Running iteration {i}...")

    # 运行Java程序
    java_process = subprocess.Popen(['java', '-jar', 'component_new encode.jar'], stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    # 等待Java程序执行完成
    java_process.communicate()

    # 获取Java程序的内存使用情况
    memory_info = psutil.Process(java_process.pid).memory_info()

    # 保存内存使用情况到文件
    with open(f'memory_{i}.txt', 'w') as file:
        file.write(f"Memory Info (Iteration {i}):\n")
        file.write(f"RSS Memory: {memory_info.rss} bytes\n")
        file.write(f"VMS Memory: {memory_info.vms} bytes\n")

    print(f"Iteration {i} completed.")

print("All iterations completed.")