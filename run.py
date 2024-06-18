import subprocess
import psutil
import os

jar_path = 'E://OneDrive - 南京大学\work/share-label/experiment/Shared-label PED component 老师说的编码方式/component_new encode.jar'

working_directory = 'E://OneDrive - 南京大学\work/share-label/experiment/Shared-label PED component 老师说的编码方式'

# 执行Java程序100次
for i in range(1, 2):
    print(f"Running iteration {i}...")

    # 运行Java程序并捕获输出
    java_process = subprocess.Popen(['java', '-jar', jar_path], cwd=working_directory, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, shell=True)

    java_pid = java_process.pid
    memory_info = psutil.Process(java_pid).memory_info()
    with open(f'memory_{i}.txt', 'w') as file:
            file.write(f"Memory Info (Iteration {i}):\n")
            file.write(f"RSS Memory: {memory_info.rss/1024/1024} bytes\n")

    java_output, java_error = java_process.communicate()

    if java_error:
        print(f"Java Error: {java_error}")

    # 保存输出到文件
    with open(f'output_{i}.txt', 'w') as file:
        file.write(java_output)

    # 打印输出到屏幕
    print(java_output)

    try:
        # 获取Java程序的内存使用情况
        memory_info = psutil.Process(java_pid).memory_info()

        # 保存内存使用情况到文件
        with open(f'memory_{i}.txt', 'w') as file:
            file.write(f"Memory Info (Iteration {i}):\n")
            file.write(f"RSS Memory: {memory_info.rss} bytes\n")
            file.write(f"VMS Memory: {memory_info.vms} bytes\n")

    except psutil.NoSuchProcess:
        print("Process not found.")

    print(f"Iteration {i} completed.")

print("All iterations completed.")