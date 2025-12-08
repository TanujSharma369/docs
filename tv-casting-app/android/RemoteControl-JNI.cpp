/*
 * RemoteControl-JNI.cpp
 * 
 * JNI bridge for sending KeypadInput SendKey commands to a commissioned Matter device.
 * Calls CastingServer::KeypadInput_SendKey() to send remote control commands.
 */

#include <jni.h>
#include <lib/support/JniReferences.h>
#include <lib/support/JniTypeWrappers.h>

#include <app/server/Server.h>
#include <platform/CHIPDeviceLayer.h>
#include <CastingServer.h>
#include <TargetVideoPlayerInfo.h>

using namespace chip;

#define JNI_METHOD(RETURN, METHOD_NAME)                                                                                            \
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_RemoteControlFragment_##METHOD_NAME

JNI_METHOD(jboolean, sendKeyToDevice)(JNIEnv * env, jobject thiz, jint keyCode)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "RemoteControl-JNI::sendKeyToDevice called with keyCode: %d", keyCode);

    // Get the active CastingPlayer (commissioned device)
    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    // Get the list of target video players
    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

    // Find an endpoint that supports KeypadInput cluster (Cluster ID 0x509)
    TargetEndpointInfo * endpoints = targetVideoPlayerInfo->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "No endpoints available");
        return JNI_FALSE;
    }
    
    TargetEndpointInfo * keypadInputEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::KeypadInput::Id))
        {
            keypadInputEndpoint = &endpoints[i];
            ChipLogProgress(AppServer, "Found KeypadInput cluster on endpoint %d", keypadInputEndpoint->GetEndpointId());
            break;
        }
    }

    if (keypadInputEndpoint == nullptr)
    {
        ChipLogError(AppServer, "No endpoint found with KeypadInput cluster support");
        return JNI_FALSE;
    }

    // Convert keyCode to CECKeyCodeEnum
    chip::app::Clusters::KeypadInput::CECKeyCodeEnum cecKeyCode = 
        static_cast<chip::app::Clusters::KeypadInput::CECKeyCodeEnum>(keyCode);

    ChipLogProgress(AppServer, "Sending KeypadInput::SendKey command to endpoint %d with keyCode %d",
                    keypadInputEndpoint->GetEndpointId(), keyCode);

    // Send the KeypadInput SendKey command
    CHIP_ERROR err = castingServer->KeypadInput_SendKey(
        keypadInputEndpoint,
        cecKeyCode,
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "KeypadInput::SendKey command succeeded");
            }
            else
            {
                ChipLogError(AppServer, "KeypadInput::SendKey command failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to send KeypadInput::SendKey command: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
