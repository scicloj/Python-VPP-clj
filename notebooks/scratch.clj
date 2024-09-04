(ns scratch
  (:require [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]
            [tablecloth.api :as tc]
            [tech.v3.tensor :as tensor]))


(require-python '[numpy :as np])

(* 3 np/pi)

(require-python '[src.UtilsMod :as utils]
                '[src.SailMod :as sail :refer [Jib Kite Main]]
                '[src.VPPMod :as vpp :refer [VPP]]
                '[src.YachtMod :as yacht :refer [Keel Rudder Yacht]]
                'builtins
                'imp)

((utils/build_interp_func "kheff") 3)

(def YD41
  (Yacht :Name "YD41",
         :Lwl 11.90 ,
         :Vol 6.05 ,
         :Bwl 3.18 ,
         :Tc 0.4 ,
         :WSA 28.2 ,
         :Tmax 2.3 ,
         :Amax 1.051 ,
         :Mass 6500 ,
         :Ff 1.5 ,
         :Fa 1.5 ,
         :Boa 4.2 ,
         :Loa 12.5 ,
         :App (py/->py-list
               [(Keel :Cu 1.00, :Cl 0.78, :Span 1.90),
                (Rudder :Cu 0.48, :Cl 0.22, :Span 1.15)]),
         :Sails (py/->py-list
                 [(Main "MN1", :P 16.60, :E 5.60, :Roach 0.1, :BAD 1.0),
                  (Jib "J1", :I 16.20, :J 5.10, :LPG 5.40, :HBI 1.8),
                  (Kite "A2", :area 150.0, :vce 9.55),
                  (Kite "A5", :area 75.0, :vce 2.75)]),))

(def vpp (VPP :Yacht YD41))

(-> vpp
    (py/py. set_analysis
        :tws_range (np/arange 4.0 30.0 1.0)
        :twa_range (np/linspace 0.0 180.0 31)))

(-> vpp
    (py/py. run :verbose true))

(def results
  (-> vpp
      (py/py. results)
      py/->jvm
      (update-keys keyword)
      (update :results tensor/->tensor)))

(-> results
    :results)
