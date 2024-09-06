(ns scratch
  (:require [libpython-clj2.python :as py]
            [libpython-clj2.require :refer [require-python]]
            [tablecloth.api :as tc]
            [tech.v3.tensor :as tensor]
            [yacht-specs :as ys]))

(comment
  (require-python '[numpy :as np])
  (* 3 np/pi)

  (require-python 'builtins)
  (builtins/abs -9)
  ;;
  )

(require-python '[src.UtilsMod :as utils]
                '[src.SailMod :as sail :refer [Jib Kite Main]]
                '[src.VPPMod :as vpp :refer [VPP]]
                '[src.YachtMod :as yacht :refer [Keel Rudder Yacht]]
                'builtins
                'imp)

(comment
  ;; testing VPP with parameters from the repo
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
                    (Kite "A5", :area 75.0, :vce 2.75)])))

  (def vpp (VPP :Yacht YD41))

  (-> vpp
      (py/py. set_analysis
              :tws_range (np/arange 4.0 30.0 1.0)
              :twa_range (np/linspace 0.0 180.0 31)))

  ;;
  )

(defn yacht-model
  [{:keys [name
           lwl
           vol
           bwl
           tc
           wsa
           tmax
           amax
           mass
           ff
           fa
           boa
           loa]

    {keel-cu :cu
     keel-cl :cl
     keel-span :span} :keel
    {rudder-cu :cu
     rudder-cl :cl
     rudder-span :span} :rudder
    {:keys [P E roach :BAD]
     main-name :name} :main
    {:keys [I J LPG :HBI]
     jib-name :name} :jib}]

  (Yacht :Name name ,
         :Lwl lwl ,
         :Vol vol,
         :Bwl bwl ,
         :Tc tc,
         :WSA wsa ,
         :Tmax tmax ,
         :Amax amax ,
         :Mass mass ,
         :Ff ff ,
         :Fa fa ,
         :Boa boa ,
         :Loa loa ,
         :App (py/->py-list
               [(Keel :Cu keel-cu, :Cl keel-cl, :Span keel-span),
                (Rudder :Cu rudder-cu, :Cl rudder-cl, :Span rudder-span)]),
         :Sails (py/->py-list
                 [(Main main-name, :P P, :E E, :Roach roach, :BAD BAD),
                  (Jib jib-name, :I I, :J J, :LPG LPG, :HBI HBI)]),))

(defn predict-vessel-speeds
  [yacht-parameters]

  (let [model
        (yacht-model yacht-parameters)

        vpp (VPP :Yacht model)
        
        _ (-> vpp
              (py/py. set_analysis
                  ;; generating vessel speeds for 4 - 30 knots at 2 knot intervals
                  :tws_range (np/arange 4.0 30.0 1.0)
                  ;; generating vessel speed for true wind angles of 0-180 degrees
                  :twa_range (np/linspace 0.0 180.0 180)))

        _   (-> vpp
                (py/py. run :verbose true))

        results     (-> vpp
                        (py/py. results)
                        py/->jvm
                        (update-keys keyword)
                        (update :results tensor/->tensor))

        _ (def dbg-result results)

        speed-column     (->> results
                              :results
                              (mapcat identity)
                              (map ffirst)
                              (assoc {} :vessel-speed))]
    (-> speed-column
        tc/dataset
        (tc/add-column :twa (:twa results) :cycle)
        (tc/add-column :tws (->> results
                                 :tws
                                 (mapcat #(repeat 180 (* 2 %))))))))

(time (-> (predict-vessel-speeds (assoc ys/yacht
                                   :keel ys/keel
                                   :rudder ys/rudder
                                   :main ys/main
                                   :jib ys/jib))
     #_(tc/write-csv! "jon30vpp.csv")))
;; "Elapsed time: 147182.649334 msecs"
