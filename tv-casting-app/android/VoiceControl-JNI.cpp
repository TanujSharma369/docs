/*
 * VoiceControl-JNI.cpp
 * 
 * JNI bridge for voice control commands - supports both KeypadInput and ApplicationLauncher
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
    extern "C" JNIEXPORT RETURN JNICALL Java_com_matter_casting_VoiceControlFragment_##METHOD_NAME

// Send KeypadInput command
JNI_METHOD(jboolean, sendKeyToDevice)(JNIEnv * env, jobject thiz, jint keyCode)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "VoiceControl-JNI::sendKeyToDevice called with keyCode: %d", keyCode);

    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

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

    chip::app::Clusters::KeypadInput::CECKeyCodeEnum cecKeyCode = 
        static_cast<chip::app::Clusters::KeypadInput::CECKeyCodeEnum>(keyCode);

    ChipLogProgress(AppServer, "Voice: Sending KeypadInput::SendKey to endpoint %d with keyCode %d",
                    keypadInputEndpoint->GetEndpointId(), keyCode);

    CHIP_ERROR err = castingServer->KeypadInput_SendKey(
        keypadInputEndpoint,
        cecKeyCode,
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "Voice: KeypadInput command succeeded");
            }
            else
            {
                ChipLogError(AppServer, "Voice: KeypadInput command failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to send KeypadInput command: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

// Launch application
JNI_METHOD(jboolean, launchApp)(JNIEnv * env, jobject thiz, jint catalogVendorId, jstring applicationId)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "VoiceControl-JNI::launchApp called");

    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

    TargetEndpointInfo * endpoints = targetVideoPlayerInfo->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "No endpoints available");
        return JNI_FALSE;
    }
    
    TargetEndpointInfo * appLauncherEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::ApplicationLauncher::Id))
        {
            appLauncherEndpoint = &endpoints[i];
            ChipLogProgress(AppServer, "Found ApplicationLauncher cluster on endpoint %d", appLauncherEndpoint->GetEndpointId());
            break;
        }
    }

    if (appLauncherEndpoint == nullptr)
    {
        ChipLogError(AppServer, "No endpoint found with ApplicationLauncher cluster");
        return JNI_FALSE;
    }

    const char * nativeApplicationId = env->GetStringUTFChars(applicationId, 0);
    
    ChipLogProgress(AppServer, "Voice: Launching app - catalogVendorId=%d, appId=%s",
                    catalogVendorId, nativeApplicationId);

    chip::app::Clusters::ApplicationLauncher::Structs::ApplicationStruct::Type application;
    application.catalogVendorID = static_cast<uint16_t>(catalogVendorId);
    application.applicationID = chip::CharSpan(nativeApplicationId, strlen(nativeApplicationId));

    CHIP_ERROR err = castingServer->ApplicationLauncher_LaunchApp(
        appLauncherEndpoint,
        application,
        chip::NullOptional,
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "Voice: ApplicationLauncher::LaunchApp succeeded");
            }
            else
            {
                ChipLogError(AppServer, "Voice: ApplicationLauncher::LaunchApp failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    env->ReleaseStringUTFChars(applicationId, nativeApplicationId);

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to launch app: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

// Stop application
JNI_METHOD(jboolean, stopApp)(JNIEnv * env, jobject thiz, jint catalogVendorId, jstring applicationId)
{
    chip::DeviceLayer::StackLock lock;
    ChipLogProgress(AppServer, "VoiceControl-JNI::stopApp called");

    CastingServer * castingServer = CastingServer::GetInstance();
    if (castingServer == nullptr)
    {
        ChipLogError(AppServer, "CastingServer instance is null");
        return JNI_FALSE;
    }

    TargetVideoPlayerInfo * targetVideoPlayerInfo = castingServer->GetActiveTargetVideoPlayer();
    if (targetVideoPlayerInfo == nullptr || !targetVideoPlayerInfo->IsInitialized())
    {
        ChipLogError(AppServer, "No active target video player found");
        return JNI_FALSE;
    }

    TargetEndpointInfo * endpoints = targetVideoPlayerInfo->GetEndpoints();
    if (endpoints == nullptr)
    {
        ChipLogError(AppServer, "No endpoints available");
        return JNI_FALSE;
    }
    
    TargetEndpointInfo * appLauncherEndpoint = nullptr;
    for (size_t i = 0; i < kMaxNumberOfEndpoints && endpoints[i].IsInitialized(); i++)
    {
        if (endpoints[i].HasCluster(chip::app::Clusters::ApplicationLauncher::Id))
        {
            appLauncherEndpoint = &endpoints[i];
            ChipLogProgress(AppServer, "Found ApplicationLauncher cluster on endpoint %d", appLauncherEndpoint->GetEndpointId());
            break;
        }
    }

    if (appLauncherEndpoint == nullptr)
    {
        ChipLogError(AppServer, "No endpoint found with ApplicationLauncher cluster");
        return JNI_FALSE;
    }

    const char * nativeApplicationId = env->GetStringUTFChars(applicationId, 0);
    
    ChipLogProgress(AppServer, "Voice: Stopping app - catalogVendorId=%d, appId=%s",
                    catalogVendorId, nativeApplicationId);

    chip::app::Clusters::ApplicationLauncher::Structs::ApplicationStruct::Type application;
    application.catalogVendorID = static_cast<uint16_t>(catalogVendorId);
    application.applicationID = chip::CharSpan(nativeApplicationId, strlen(nativeApplicationId));

    CHIP_ERROR err = castingServer->ApplicationLauncher_StopApp(
        appLauncherEndpoint,
        application,
        [](CHIP_ERROR err) {
            if (err == CHIP_NO_ERROR)
            {
                ChipLogProgress(AppServer, "Voice: ApplicationLauncher::StopApp succeeded");
            }
            else
            {
                ChipLogError(AppServer, "Voice: ApplicationLauncher::StopApp failed: %" CHIP_ERROR_FORMAT, err.Format());
            }
        });

    env->ReleaseStringUTFChars(applicationId, nativeApplicationId);

    if (err != CHIP_NO_ERROR)
    {
        ChipLogError(AppServer, "Failed to stop app: %" CHIP_ERROR_FORMAT, err.Format());
        return JNI_FALSE;
    }

    return JNI_TRUE;
}
