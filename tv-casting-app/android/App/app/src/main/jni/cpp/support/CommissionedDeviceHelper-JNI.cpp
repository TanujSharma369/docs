/*
 *   Copyright (c) 2024 Project CHIP Authors
 *   All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

#include <jni.h>
#include <lib/support/CHIPJNIError.h>
#include <lib/support/JniReferences.h>
#include <lib/support/JniTypeWrappers.h>
#include <app/server/Server.h>
#include <credentials/FabricTable.h>
#include <lib/support/logging/CHIPLogging.h>

#define JNI_METHOD(RETURN, METHOD_NAME) \
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_CommissionedDeviceHelper_##METHOD_NAME

using namespace chip;

JNI_METHOD(jboolean, hasCommissionedDevice)(JNIEnv * env, jclass)
{
    ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::hasCommissionedDevice() called");
    
    chip::FabricTable & fabricTable = chip::Server::GetInstance().GetFabricTable();
    
    // Check if there are any fabrics (commissioned devices)
    for (const auto & fabricInfo : fabricTable)
    {
        if (fabricInfo.IsInitialized())
        {
            ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::hasCommissionedDevice() Found fabric with index %d, nodeId: 0x" ChipLogFormatX64,
                          fabricInfo.GetFabricIndex(), ChipLogValueX64(fabricInfo.GetNodeId()));
            return JNI_TRUE;
        }
    }
    
    ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::hasCommissionedDevice() No fabrics found");
    return JNI_FALSE;
}

JNI_METHOD(jobjectArray, getCommissionedDeviceInfo)(JNIEnv * env, jclass)
{
    ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::getCommissionedDeviceInfo() called");
    
    chip::JniReferences::GetInstance().GetEnvForCurrentThread();
    
    chip::FabricTable & fabricTable = chip::Server::GetInstance().GetFabricTable();
    
    // Count initialized fabrics
    size_t fabricCount = 0;
    for (const auto & fabricInfo : fabricTable)
    {
        if (fabricInfo.IsInitialized())
        {
            fabricCount++;
        }
    }
    
    ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::getCommissionedDeviceInfo() Found %zu fabrics", fabricCount);
    
    if (fabricCount == 0)
    {
        return nullptr;
    }
    
    // Create String array to hold info
    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray infoArray = env->NewObjectArray(fabricCount, stringClass, nullptr);
    
    size_t index = 0;
    for (const auto & fabricInfo : fabricTable)
    {
        if (fabricInfo.IsInitialized())
        {
            char infoStr[256];
            snprintf(infoStr, sizeof(infoStr), 
                    "FabricIndex:%d,NodeId:0x%llx,FabricId:0x%llx",
                    fabricInfo.GetFabricIndex(),
                    static_cast<unsigned long long>(fabricInfo.GetNodeId()),
                    static_cast<unsigned long long>(fabricInfo.GetFabricId()));
            
            jstring jInfo = env->NewStringUTF(infoStr);
            env->SetObjectArrayElement(infoArray, index, jInfo);
            env->DeleteLocalRef(jInfo);
            
            ChipLogProgress(AppServer, "CommissionedDeviceHelper-JNI::getCommissionedDeviceInfo() %s", infoStr);
            index++;
        }
    }
    
    return infoArray;
}
