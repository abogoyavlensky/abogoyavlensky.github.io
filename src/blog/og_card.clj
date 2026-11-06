(ns blog.og-card
  "Draw social preview cards with Java2D.

  Fonts load lazily: the CI image has no libfreetype, so nothing here may touch
  a font while the namespace is loading."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (java.awt Color Font Graphics2D RenderingHints TexturePaint)
           (java.awt.font TextAttribute)
           (java.awt.geom Ellipse2D$Double Rectangle2D$Double)
           (java.awt.image BufferedImage)
           (javax.imageio ImageIO)))


(System/setProperty "java.awt.headless" "true")


(def ^:private WIDTH 1200)
(def ^:private HEIGHT 630)
(def ^:private PADDING 80)

(def ^:private COLOR-TITLE (Color. 0x1a202c))
(def ^:private COLOR-AUTHOR (Color. 0x2d3748))
(def ^:private COLOR-SITE (Color. 0x4a5568))
(def ^:private COLOR-ACCENT (Color. 0x667eea))

(def ^:private SITE-NAME "bogoyavlensky.com")
(def ^:private SITE-SIZE 30)
(def ^:private ACCENT-TOP 134)
(def ^:private ACCENT-WIDTH 72)
(def ^:private ACCENT-HEIGHT 4)

(def ^:private TITLE-SIZES [72 60])
(def ^:private TITLE-MAX-LINES 3)
(def ^:private TITLE-LINE-HEIGHT 1.18)

(def ^:private AUTHOR "Andrey Bogoyavlenskiy")
(def ^:private AUTHOR-SIZE 28)
(def ^:private AVATAR-SIZE 56)
(def ^:private AVATAR-GAP 18)
(def ^:private AVATAR-TOP (- HEIGHT PADDING AVATAR-SIZE))

(def ^:private FONT-TITLE "fonts/Inter-Bold.ttf")
(def ^:private FONT-AUTHOR "fonts/Inter-Medium.ttf")
(def ^:private FONT-SITE "fonts/JetBrainsMono-Regular.ttf")
(def ^:private AVATAR-PATH "public/images/my_photo_512x512.JPG")


(def ^:private load-font
  (memoize
    (fn [path]
      (with-open [stream (io/input-stream (io/resource path))]
        (Font/createFont Font/TRUETYPE_FONT stream)))))


(defn- font
  ^Font [path size]
  (let [^Font sized (.deriveFont ^Font (load-font path) (float size))]
    (.deriveFont sized ^java.util.Map {TextAttribute/KERNING TextAttribute/KERNING_ON})))


(defn wrap-lines
  "Split text into lines no wider than max-width, breaking only between words.
  `measure` returns the width of a string. A single word wider than max-width
  stays on its own line."
  [text max-width measure]
  (reduce
    (fn [lines word]
      (let [candidate (str (peek lines) " " word)]
        (cond
          (empty? lines) [word]
          (<= (measure candidate) max-width) (conj (pop lines) candidate)
          :else (conj lines word))))
    []
    (str/split text #"\s+")))


(defn- fit-title
  "Pick the largest title size that fits in the allowed number of lines."
  [^Graphics2D g title]
  (let [max-width (- WIDTH (* 2 PADDING))
        fits (for [size TITLE-SIZES
                   :let [title-font (font FONT-TITLE size)
                         metrics (.getFontMetrics g title-font)
                         measure #(.stringWidth metrics %)
                         lines (wrap-lines title max-width measure)]
                   :when (and (<= (count lines) TITLE-MAX-LINES)
                           (every? #(<= (measure %) max-width) lines))]
               {:font title-font
                :size size
                :lines lines})]
    (or (first fits)
      (throw (ex-info "Title does not fit on a social card, shorten it or set :og-image"
               {:title title})))))


(defn- draw-title
  "Center the title block vertically between the accent line and the avatar."
  [^Graphics2D g title]
  (let [{:keys [^Font font size lines]} (fit-title g title)
        line-height (* size TITLE-LINE-HEIGHT)
        top (+ ACCENT-TOP ACCENT-HEIGHT)
        block-height (* line-height (count lines))
        block-top (+ top (/ (- AVATAR-TOP top block-height) 2))
        ascent (.getAscent (.getFontMetrics g font))]
    (.setFont g font)
    (.setColor g COLOR-TITLE)
    (doseq [[index line] (map-indexed vector lines)]
      (.drawString g ^String line
        (float PADDING)
        (float (+ block-top ascent (* index line-height)))))))


(defn- scaled-image
  "Halve repeatedly before the last step, so a large photo does not alias."
  ^BufferedImage [^BufferedImage source size]
  (let [target (if (> (.getWidth source) (* 2 size))
                 (quot (.getWidth source) 2)
                 size)
        image (BufferedImage. target target BufferedImage/TYPE_INT_RGB)
        g (.createGraphics image)]
    (.setRenderingHint g RenderingHints/KEY_INTERPOLATION RenderingHints/VALUE_INTERPOLATION_BILINEAR)
    (.drawImage g source 0 0 target target nil)
    (.dispose g)
    (if (= target size)
      image
      (recur image size))))


(defn- draw-avatar
  "Fill an antialiased circle with the photo, because a clip leaves jagged edges."
  [^Graphics2D g]
  (let [source (ImageIO/read (io/resource AVATAR-PATH))
        avatar (scaled-image source AVATAR-SIZE)
        left (double PADDING)
        top (double AVATAR-TOP)
        size (double AVATAR-SIZE)]
    (.setPaint g (TexturePaint. avatar (Rectangle2D$Double. left top size size)))
    (.fill g (Ellipse2D$Double. left top size size))))


(defn- draw-author
  [^Graphics2D g]
  (let [author-font (font FONT-AUTHOR AUTHOR-SIZE)
        metrics (.getFontMetrics g author-font)
        baseline (+ AVATAR-TOP
                   (/ (+ AVATAR-SIZE (.getAscent metrics) (- (.getDescent metrics))) 2))]
    (.setFont g author-font)
    (.setColor g COLOR-AUTHOR)
    (.drawString g ^String AUTHOR
      (float (+ PADDING AVATAR-SIZE AVATAR-GAP))
      (float baseline))))


(defn- draw-header
  [^Graphics2D g]
  (let [site-font (font FONT-SITE SITE-SIZE)]
    (.setFont g site-font)
    (.setColor g COLOR-SITE)
    (.drawString g ^String SITE-NAME
      (float PADDING)
      (float (+ PADDING (.getAscent (.getFontMetrics g site-font)))))
    (.setColor g COLOR-ACCENT)
    (.fillRect g PADDING ACCENT-TOP ACCENT-WIDTH ACCENT-HEIGHT)))


(defn- set-quality-hints
  [^Graphics2D g]
  (doto g
    (.setRenderingHint RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
    (.setRenderingHint RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
    (.setRenderingHint RenderingHints/KEY_FRACTIONALMETRICS RenderingHints/VALUE_FRACTIONALMETRICS_ON)
    (.setRenderingHint RenderingHints/KEY_RENDERING RenderingHints/VALUE_RENDER_QUALITY)
    (.setRenderingHint RenderingHints/KEY_STROKE_CONTROL RenderingHints/VALUE_STROKE_PURE)))


(defn render-card
  "Draw a 1200×630 card for the title."
  ^BufferedImage [title]
  (let [image (BufferedImage. WIDTH HEIGHT BufferedImage/TYPE_INT_RGB)
        g (.createGraphics image)]
    (try
      (set-quality-hints g)
      (.setColor g Color/WHITE)
      (.fillRect g 0 0 WIDTH HEIGHT)
      (draw-header g)
      (draw-title g title)
      (draw-avatar g)
      (draw-author g)
      image
      (finally
        (.dispose g)))))


(defn write-card!
  "Render a card for the title and write it to out-path as a PNG."
  [title out-path]
  (let [file (io/file out-path)]
    (io/make-parents file)
    (ImageIO/write (render-card title) "png" file)))
