package org.firstinspires.ftc.teamcode.opMode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Blinker;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.PwmControl;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.common.BotConstants;

@TeleOp (group = "Competition", name = "Field Centric 2Player")

// Next line will prevent code from building and showing up on Control Hub
// @Disabled
// comment

public class FieldCentric2P extends LinearOpMode {
  private Blinker control_Hub;
  private Servo bottomArmServo;
  private IMU imu;
  private Servo topArmServo;
  private Servo wristPanServo;
  private ElapsedTime runtime = new ElapsedTime();
  private DcMotor frontLeftMotor = null; //front left
  private DcMotor frontRightMotor = null; //front right
  private DcMotor backLeftMotor = null; //back left
  private DcMotor backRightMotor = null; //back right
  private DcMotor armmotor = null;
  private Servo droneServo = null;
  private Servo hookServo = null;

  //ALL Common CONSTANTS MOVED TO BotConstants Class

  double botHeading = 0;


  @Override
  public void runOpMode() {
    //read hardware configurations
    control_Hub = hardwareMap.get(Blinker.class, "Control Hub");
    bottomArmServo = hardwareMap.get(Servo.class, "bottomArmServo");
    topArmServo = hardwareMap.get(Servo.class, "topArmServo");
    wristPanServo = hardwareMap.get(Servo.class, "wristPanServo");
    frontLeftMotor = hardwareMap.get(DcMotor.class, "motor1");
    frontRightMotor = hardwareMap.get(DcMotor.class, "motor2");
    backLeftMotor = hardwareMap.get(DcMotor.class, "motor3");
    backRightMotor = hardwareMap.get(DcMotor.class, "motor4");
    armmotor = hardwareMap.get(DcMotor.class, "armcontrol");
    droneServo = hardwareMap.get(Servo.class, "droneServo");
    hookServo = hardwareMap.get(Servo.class, "hookServo");
    imu = hardwareMap.get(IMU.class, "imu");

    //define initial values for variables
    double lTrigger;
    double rTrigger;
    double lStickX;
    double lStickY;
    double rStickX;
    double lStickY2; //Gamepad 2 left stick
    double rStickY2; //Gamepad 2 right stick
    boolean left_Stick_Button = false;
    boolean bottomFingerServoOpen = false;
    boolean topFingerServoOpen = false;
    boolean robotHanging = false;
    //Wrist initial position is folded
    double wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
    // By setting these values to new Gamepad(), they will default to all
    // boolean values as false and all float values as 0
    int previousArmPos;
    boolean dPadPressed = false;

    Gamepad currentGamepad1 = new Gamepad();
    Gamepad currentGamepad2 = new Gamepad();

    Gamepad previousGamepad1 = new Gamepad();
    Gamepad previousGamepad2 = new Gamepad();

    double slow_mode = BotConstants.DRIVE_SLOW_MODE;

    PwmControl hookServoPWM = (PwmControl) hookServo;

    telemetry.addData("Status", "Initializing...");
    telemetry.update();

    //Initialized the motors
    frontLeftMotor.setPower(0);
    frontLeftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    frontLeftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

    frontRightMotor.setPower(0);
    frontRightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    frontRightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    frontRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

    backLeftMotor.setPower(0);
    backLeftMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    backLeftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

    backRightMotor.setPower(0);
    backRightMotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
    backRightMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    backRightMotor.setDirection(DcMotorSimple.Direction.REVERSE);

    armmotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    armmotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER); // jw test

    //Initialize arm motor
    armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR);
    armmotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    armmotor.setPower(1);

    telemetry.addData("Arm Motor init", "Arm Motor decoder: %d", armmotor.getCurrentPosition());
    telemetry.addData("Arm Motor init", "run mode: %s", armmotor.getMode().toString());


    //initialize wristPanServo and drone servo
    wristPanServo.setPosition(BotConstants.WRIST_PAN_SERVO_FOLDED);
    droneServo.setPosition(BotConstants.DRONE_POSITION_ARMED);
    hookServo.setPosition(BotConstants.HOOK_POS_RETRACT);
    telemetry.addData("Wrist servo Position:", "%f", wristPanServo.getPosition());
    telemetry.addData("Dronw servo Position", "%f", droneServo.getPosition());
    telemetry.addData("Hook servo Position", "%f", hookServo.getPosition());

    //initialize both hand servos
    // Reverse Top Servo
    topArmServo.setDirection(Servo.Direction.REVERSE);
    topArmServo.setPosition(BotConstants.TOP_ARM_SERVO_CLOSE);
    bottomArmServo.setPosition(BotConstants.BOTTOM_ARM_SERVO_CLOSE);
    telemetry.addData("top finger servo Position", "%f", topArmServo.getPosition());
    telemetry.addData("bottom finger servo Position", "%f", bottomArmServo.getPosition());

    //init imu
    // commented out to use the Yaw from Automation
    /*IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
      RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
      RevHubOrientationOnRobot.UsbFacingDirection.UP
    ));
    imu.initialize(parameters);

    imu.resetYaw();*/

    telemetry.addData("Status", "Initialized");
    telemetry.update();

    // Wait for the game to start (driver presses PLAY)
    waitForStart();

    if (isStopRequested()) {
      return;
    }

    // run until the end of the match (driver presses STOP)
    while (opModeIsActive()) {

      previousGamepad1.copy(currentGamepad1);
      previousGamepad2.copy(currentGamepad2);

      currentGamepad1.copy(gamepad1);
      currentGamepad2.copy(gamepad2);

      if (!robotHanging) {
        //Driving control from Gamepad 1
        //mecanum drive train
        // TODO: ADD DRIVE_SLOW_MODE Toggle between 1.0 and BotConstants.DRIVE_SLOW_MODE
        lStickX = slow_mode * (gamepad1.left_stick_x * Math.abs(gamepad1.left_stick_x));
        lStickY = slow_mode * (-gamepad1.left_stick_y * Math.abs(gamepad1.left_stick_y));
        rStickX = slow_mode * (gamepad1.right_stick_x * Math.abs(gamepad1.right_stick_x));

        //If the field centric drive lost direction, push Back button to reset heading to Bot Front
        if (currentGamepad1.back && !previousGamepad1.back) {
          IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                  RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
                  RevHubOrientationOnRobot.UsbFacingDirection.UP
          ));
          imu.initialize(parameters);

          imu.resetYaw();
        }

        botHeading = imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);

        telemetry.addData("Stick Powers", ":lStickX: %.2f, lStickY: %.2f, RStickX:%.2f", lStickX, lStickY, rStickX);
        telemetry.addData("imu", "yaw: %.2f", botHeading);

        setMotorPowers(lStickX, lStickY, rStickX, botHeading);

        // Gamepad 2 controls everything but driving

        // Robot arm is controlled by the left stick y on Gamepad 2
        lStickY2 = -gamepad2.left_stick_y * Math.abs(gamepad2.left_stick_y);
        telemetry.addData("StickY2", "%.5f", lStickY2);
        if (lStickY2 < 0) {
          armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR);
          armmotor.setPower(Math.abs(lStickY2));
          dPadPressed = false;
        } else if (lStickY2 > 0) {
          armmotor.setTargetPosition(BotConstants.ARM_POS_MAX);
          armmotor.setPower(Math.abs(lStickY2));
          dPadPressed = false;
        } else {

          // dpad up set the wrist and arm to a default deploy position.
          // the driver can use gamepad2 left stick to fine tune the position
          // dpad left set the arm at driving height
          // dpad down set the arm at pick up position
          if (currentGamepad2.dpad_up && !previousGamepad2.dpad_up) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_L2_DEPLOY;
            wristPanServo.setPosition(wristPanPos);
            //setArmPosition(BotConstants.ARM_POS_L2_DROP, BotConstants.ARM_POWER);
            armmotor.setTargetPosition(BotConstants.ARM_POS_L2_DROP);
            armmotor.setPower(BotConstants.ARM_POWER);
            dPadPressed = true;
          }
          // dpad down set the arm at pick up position dpad left set the arm at driving height
          else if (currentGamepad2.dpad_down && !previousGamepad2.dpad_down) {
            wristPanPos = BotConstants.WRIST_PICK_UP;
            if (armmotor.getCurrentPosition() >= 6000) {
              //setArmPosition(BotConstants.ARM_POS_FLOOR_TELEOP, BotConstants.ARM_POWER);
              armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR_TELEOP);
              wristPanServo.setPosition(wristPanPos);
            }
            else {
              wristPanServo.setPosition(wristPanPos);
              armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR_TELEOP);
              //setArmPosition(BotConstants.ARM_POS_FLOOR_TELEOP, BotConstants.ARM_POWER);
            }
            dPadPressed = true;
          }
          // dpad left set the arm at driving height
          else if (currentGamepad2.dpad_left && !previousGamepad2.dpad_left) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
            wristPanServo.setPosition(wristPanPos);
            //setArmPosition(BotConstants.ARM_POS_DRIVE, BotConstants.ARM_POWER);
            armmotor.setTargetPosition(BotConstants.ARM_POS_DRIVE);
            dPadPressed = true;
          }
          else if (dPadPressed) {
            if (Math.abs((armmotor.getCurrentPosition() - armmotor.getTargetPosition())) <= 2) {
              //reached dpad destination
              armmotor.setTargetPosition(armmotor.getCurrentPosition());
              dPadPressed = false;
            }
          }
          else {
            armmotor.setTargetPosition(armmotor.getCurrentPosition());
          }
        }


        telemetry.addData("Arm Motor Position", "Arm Motor encoder: %d", armmotor.getCurrentPosition());
        telemetry.addData("Arm Motor Position", "run mode: %s", armmotor.getMode().toString());

/*
          // dpad up set the wrist and arm to a default deploy position.
          // the driver can use gamepad2 left stick to change the position
          // dpad left set the arm at driving height
          // dpad down set the arm at pick up position
          if (currentGamepad2.dpad_up && !previousGamepad2.dpad_up) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_L2_DEPLOY;
            wristPanServo.setPosition(wristPanPos);
            //setArmPosition(BotConstants.ARM_POS_L2_DROP, BotConstants.ARM_POWER);
            armmotor.setTargetPosition(BotConstants.ARM_POS_L2_DROP);
          }
          // dpad down set the arm at pick up position dpad left set the arm at driving height
          if (currentGamepad2.dpad_down && !previousGamepad2.dpad_down) {
            wristPanPos = BotConstants.WRIST_PICK_UP;
            if (armmotor.getCurrentPosition() >= 6000) {
              //setArmPosition(BotConstants.ARM_POS_FLOOR_TELEOP, BotConstants.ARM_POWER);
              armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR_TELEOP);
              wristPanServo.setPosition(wristPanPos);
            }
            else {
              wristPanServo.setPosition(wristPanPos);
              armmotor.setTargetPosition(BotConstants.ARM_POS_FLOOR_TELEOP);
              //setArmPosition(BotConstants.ARM_POS_FLOOR_TELEOP, BotConstants.ARM_POWER);
            }
          }
          // dpad left set the arm at driving height
          if (currentGamepad2.dpad_left && !previousGamepad2.dpad_left) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
            wristPanServo.setPosition(wristPanPos);
            //setArmPosition(BotConstants.ARM_POS_DRIVE, BotConstants.ARM_POWER);
            armmotor.setTargetPosition(BotConstants.ARM_POS_DRIVE);
          }
          armmotor.setPower(BotConstants.ARM_POWER);

            // dpad right set the arm at hanging height
          /*if (currentGamepad2.dpad_right && !previousGamepad2.dpad_right){
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
            wristPanServo.setPosition(wristPanPos);
            //setArmPosition(BotConstants.ARM_POS_HANG, BotConstants.ARM_POWER);
            armmotor.setTargetPosition(BotConstants.ARM_POS_HANG;
          }*/



        //servo slot 0 & 1 are for the finger controls
        if (currentGamepad2.right_bumper && !previousGamepad2.right_bumper) {
          if (!topFingerServoOpen) {
            topArmServo.setPosition(BotConstants.TOP_ARM_SERVO_OPEN);
            topFingerServoOpen = true;
          }
          else {
            topArmServo.setPosition(BotConstants.TOP_ARM_SERVO_CLOSE);
            topFingerServoOpen = false;
          }
        }

        if (currentGamepad2.right_trigger > 0 && !(previousGamepad2.right_trigger > 0)) {
          if (!bottomFingerServoOpen) {
            bottomArmServo.setPosition(BotConstants.BOTTOM_ARM_SERVO_OPEN);
            bottomFingerServoOpen = true;
          }
          else {
            bottomArmServo.setPosition(BotConstants.BOTTOM_ARM_SERVO_CLOSE);
            bottomFingerServoOpen = false;
          }
        }

        if (currentGamepad2.left_bumper && !previousGamepad2.left_bumper) {
          if (!topFingerServoOpen) {
            topArmServo.setPosition(BotConstants.TOP_ARM_SERVO_OPEN);
            topFingerServoOpen = true;
            bottomArmServo.setPosition(BotConstants.BOTTOM_ARM_SERVO_OPEN);
            bottomFingerServoOpen = true;
          } else {
            topArmServo.setPosition(BotConstants.TOP_ARM_SERVO_CLOSE);
            topFingerServoOpen = false;
            bottomArmServo.setPosition(BotConstants.BOTTOM_ARM_SERVO_CLOSE);
            bottomFingerServoOpen = false;
          }
        }
        telemetry.addData("top finger servo Position", "%f", topArmServo.getPosition());
        telemetry.addData("bottom finger servo Position", "%f", bottomArmServo.getPosition());

        //wrist servo is in slot 2
        rStickY2 = -gamepad2.right_stick_y * Math.abs(gamepad2.right_stick_y);
        if (rStickY2 > 0) {
          wristPanPos += BotConstants.WRIST_PAN_SERVO_SPEED;
          if (wristPanPos > BotConstants.WRIST_PAN_SERVO_FOLDED) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
          }
        }
        else if (rStickY2 < 0 ) {
          wristPanPos -= BotConstants.WRIST_PAN_SERVO_SPEED;
          if (wristPanPos < BotConstants.WRIST_PAN_SERVO_FLOOR) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FLOOR;
          }
        }
        else {
          //do nothing when the stick is at 0 position
        }

        wristPanServo.setPosition(wristPanPos);
        telemetry.addData("Wrist Servos", "wristPanPos %.3f", wristPanServo.getPosition());

        //camera is servo in slot 3. Not implemented in TeleOp

        //airplane launcher servo is in slot 4
        if (currentGamepad2.y && !previousGamepad2.y) {
          if (droneServo.getPosition() == BotConstants.DRONE_POSITION_ARMED)
            droneServo.setPosition(BotConstants.DRONE_POSITION_LAUNCH);
          else
            droneServo.setPosition(BotConstants.DRONE_POSITION_ARMED);
        }
        telemetry.addData("Drone Servos", "%.3f", droneServo.getPosition());

        //hook servo is in slot 5
        if (currentGamepad1.x && !previousGamepad1.x) {
          if (hookServo.getPosition() == BotConstants.HOOK_POS_RETRACT) {
            wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
            wristPanServo.setPosition(wristPanPos);
            setArmPosition(BotConstants.ARM_POS_HANG, BotConstants.ARM_POWER);
            hookServo.setPosition(BotConstants.HOOK_POS_DEPLOY);
          }
          else
            hookServo.setPosition(BotConstants.HOOK_POS_RETRACT);
        }
        telemetry.addData("Hook servo Position", ":%f", hookServo.getPosition());

        if (currentGamepad1.start && !previousGamepad1.start) {
          // make sure the wrist is in fold position
          wristPanPos = BotConstants.WRIST_PAN_SERVO_FOLDED;
          wristPanServo.setPosition(wristPanPos);
          // Disable the hook servo to disengage the servo connected to the hook.
          // hookServo.getController().pwmDisable();
          hookServoPWM.setPwmDisable();
          setArmPosition(BotConstants.ARM_POS_FLOOR,BotConstants.ARM_POWER);
          robotHanging = true;
        }
        telemetry.addData("Servo Power", ":%s", hookServo.getController().getPwmStatus().toString());
        telemetry.update();

        previousArmPos = armmotor.getCurrentPosition();

      }
      else {
        //robot is hanging
        if (currentGamepad1.start && !previousGamepad1.start) {
          robotHanging = false;
          hookServoPWM.setPwmEnable();
        }
        telemetry.addData("Servo Power", ":%s", hookServo.getController().getPwmStatus().toString());
        telemetry.update();

      }
    }
  }
  public void setMotorPowers(double x, double y, double rx, double heading) {
    double rotX = x * Math.cos(-heading) - y * Math.sin(-heading);
    double rotY = x * Math.sin(-heading) + y * Math.cos(-heading);

    // put strafing factors here
    rotX = rotX * 1;
    rotY = rotY * 1;

    double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(rx), 1);

    double frontLeftPower = (rotY + rotX + rx)/denominator;
    double backLeftPower = (rotY - rotX + rx)/denominator;
    double frontRightPower = (rotY - rotX - rx)/denominator;
    double backRightPower = (rotY + rotX - rx)/denominator;

    frontLeftMotor.setPower(frontLeftPower);
    backLeftMotor.setPower(backLeftPower);
    frontRightMotor.setPower(frontRightPower);
    backRightMotor.setPower(backRightPower);
  }
  public void setArmPosition(int position, double speed) {
    armmotor.setTargetPosition(position);
    armmotor.setPower(speed);

    for (int i=0; i<5; i++) {
      while (armmotor.isBusy()) {
        sleep(10);
      }
    }
  }

}
